import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.Date;


public class Data implements Closeable {
    public static final String PHONE_REGEX = "^(\\d{3})-(\\d{3})-(\\d{4})$";
    final String DELETE_ORDER = "DELETE FROM VendorSale WHERE SaleID = %d";
    final String CREATE_VENDOR_SALE_SHIPMENT = "INSERT INTO VendorSaleShippedIn (SaleID, ShipmentID) VALUES (%d, %d)";
    final String CREATE_SHIPMENT =
            "INSERT INTO Shipment (ShipmentID, ArrivalDate, SRCSTREET, SRCCITY, SRCSTATE, SRCZIP, DSTSTREET, DSTCITY, DSTSTATE, DSTZIP) " +
                    "VALUES (%d, %d, '%s', '%s', '%s', %d, '%s', '%s', '%s', %d)";
    final String GET_VENDOR_SALE = String.join(" ",
            "WITH VendorSaleShipments as (",
            "SELECT VendorID, VendorSale.SaleID, StoreID, DateOfSale, Unpacked, ShipmentID",
            "FROM VendorSale",
            "    INNER JOIN VendorSaleShippedIn",
            "        ON VendorSale.SaleID = VendorSaleShippedIn.SaleID )",

            "SELECT * FROM VendorSaleShipments",
            "    INNER JOIN PhysicalStore",
            "        ON VendorSaleShipments.StoreID = PhysicalStore.StoreID",
            "    INNER JOIN Shipment",
            "        ON VendorSaleShipments.ShipmentID = Shipment.ShipmentID",
            "WHERE VendorID = %d "
    );
    final String GET_VENDOR_SALE_PRODUCTS = String.join(" ",
            "WITH VendorSaleProductWithID AS (",
            "    SELECT * FROM VendorSale",
            "        INNER JOIN VendorSaleProduct",
            ")",

            "SELECT VendorID, SaleID, Product.ProductUPCCode, ProductName, Quantity, Price, SUM(Price * Quantity)",
            "FROM VendorSaleProductWithID",
            "    INNER JOIN Product",
            "        ON Product.ProductUPCCode = VendorSaleProductWithID.ProductUPCCode",
            "WHERE VendorSaleProductWithID.VendorID = %d",
            "GROUP BY Product.ProductUPCCode, Quantity, SaleID, Price",
            "ORDER BY SaleID"
    );
    final String GET_VENDOR_SALE_UNPROCESSED = String.join(" ",
            "WITH VendorSalesWithoutShipments AS (",
            "      SELECT VendorID, VendorSale.SaleID, StoreID, DateOfSale, Unpacked",
            "      FROM VendorSale",
            "      WHERE VendorID = %d",

            "      EXCEPT",

            "      SELECT VendorID, VendorSale.SaleID, StoreID, DateOfSale, Unpacked",
            "      FROM VendorSale",
            "      INNER JOIN VendorSaleShippedIn",
            "      ON VendorSale.SaleID = VendorSaleShippedIn.SaleID )",

            "SELECT * FROM VendorSalesWithoutShipments",
            "  INNER JOIN PhysicalStore",
            "      ON VendorSalesWithoutShipments.StoreID = PhysicalStore.StoreID"
    );
    final String VENDOR_SALE_IN_TRANSIT = GET_VENDOR_SALE + "AND %d < Shipment.ArrivalDate";
    private Connection dbConnection;
    private int onlineCustomerId = 0;
    private int anonymousCustomerId = -1;
    private String currentSQLStatement = "";
    private int currentStoreId = 0;
    // True means browse by category, false means browse by search phrase
    private boolean browseByCategory = false;
    private ProductType currentCategory;
    private String currentSearchPhrase;
    private EditableOnlineField currentField;
    // Can be used for when the user selects a product and that choice has to be remembered for the next screen
    private int currentProductUpcCode;
    // Key: product's primary key (upc code), Value: how much is in their cart
    private Map<Integer, Map<Integer, Integer>> allCustomersCart = new HashMap<>();
    private boolean usePoints = false;
    private float cash = 0;
    private float totalPrice = 0;
    private int vendorID;
    private int orderToProcess;
    private int orderToDelete;
    private List<VendorOrder> vendorOrders;

    public Data(String location, String user, String password) throws ClassNotFoundException, SQLException {
        // Only connect to the database if it exists
        String url = String.format("jdbc:h2:%s;IFEXISTS=TRUE", location);
        Class.forName("org.h2.Driver");
        dbConnection = DriverManager.getConnection(url, user, password);

        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("CREATE TRIGGER IF NOT EXISTS ReorderTrigger AFTER UPDATE ON StoreInventory FOR EACH ROW CALL \"%s\"", ReorderTrigger.class.getName()));
    }

    public void addToCart(int onlineCustomerId, int upcCode, int quantity) {
        Map<Integer, Integer> customerCart = allCustomersCart.get(onlineCustomerId);
        if (customerCart.containsKey(upcCode)) {
            customerCart.put(upcCode, customerCart.get(upcCode) + quantity);
        } else {
            customerCart.put(upcCode, quantity);
        }
    }

    public void checkIfStoreStocksProduct(int upcCode) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT COUNT(*) FROM StoreInventory WHERE StoreID = %d AND ProductID = %d", currentStoreId, upcCode));
        result.next();
        if (result.getInt("COUNT(*)") == 0) {
            throw new SQLException(String.format("Store %d does not stock products with UPC code %d.", currentStoreId, upcCode));
        }
    }

    public void addCartToCarts(int onlineCustomerId) {
        if (!allCustomersCart.containsKey(onlineCustomerId)) {
            allCustomersCart.put(onlineCustomerId, new HashMap<>());
        }
    }

    public void setCartQuantity(int onlineCustomerId, int upcCode, int quantity) {
        allCustomersCart.get(onlineCustomerId).put(upcCode, quantity);
    }

    public void removeCustomerFromCart(int onlineCustomerId) {
        allCustomersCart.remove(onlineCustomerId);
    }

    public List<Integer> getArrivedPackedSales() throws SQLException {
        // Current epoch timestamp
        Instant instant = Instant.now();
        long currentTimestamp = instant.getEpochSecond();

        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT * FROM VendorSale NATURAL JOIN VendorSaleShippedIn NATURAL JOIN Shipment WHERE ArrivalDate <= %d and Unpacked = FALSE and StoreID = %d;", currentTimestamp, currentStoreId));
        List<Integer> arrivedPackedSales = new ArrayList<>();
        while (result.next()) {
            arrivedPackedSales.add(result.getInt("SaleID"));
        }
        return arrivedPackedSales;
    }

    public List<String> getVendorSaleProducts(int vendorSaleId) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT * FROM VendorSaleProduct NATURAL JOIN PRODUCT WHERE VendorSaleId = %d", vendorSaleId));

        List<String> products = new ArrayList<>();
        while (result.next()) {
            products.add(String.format("%-50s x%d", result.getString("ProductName"), result.getInt("Quantity")));
        }
        return products;
    }

    public void unpackVendorSale(int vendorSaleId) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT * FROM VendorSaleProduct NATURAL JOIN PRODUCT WHERE VendorSaleId = %d", vendorSaleId));
        while (result.next()) {
            int upcCode = result.getInt("ProductUPCCode");
            int quantity = result.getInt("Quantity");

            // Find out if the store stocks this item or not
            statement = dbConnection.createStatement();
            ResultSet productInInventory = statement.executeQuery(String.format("SELECT COUNT(*) FROM StoreInventory WHERE StoreId = %d AND ProductID = %d;", currentStoreId, upcCode));
            productInInventory.next();
            boolean in = productInInventory.getInt("COUNT(*)") == 1 ? true : false;

            // If the store inventory already stocks this item, just update their quantity
            // Otherwise, insert the items into their inventory
            if (in) {
                statement = dbConnection.createStatement();
                statement.execute(String.format("UPDATE StoreInventory SET Quantity = Quantity + %d WHERE StoreID = %d and ProductID = %d;", quantity, currentStoreId, upcCode));
            } else {
                // For new items (never before in the stores inventory), the default price is 10% more than they bought it for and default threshold is new quantity - 10
                statement = dbConnection.createStatement();
                int vendorPrice = result.getInt("Price");
                statement.execute(String.format("INSERT INTO StoreInventory VALUES (%d, %d, %d, %d, %d);", currentStoreId, upcCode, quantity, (int) (vendorPrice * 1.1), quantity - 10));
            }

        }
        statement.execute(String.format("UPDATE VendorSale SET Unpacked = TRUE WHERE SaleID = %d;", vendorSaleId));
    }

    public void setBrowseByCategory(boolean browseByCategory) {
        this.browseByCategory = browseByCategory;
    }

    public void removeProductFromCart(int onlineCustomerId, int upcCode) {
        allCustomersCart.get(onlineCustomerId).remove(upcCode);
    }

    public void setCurrentStoreId(int id) throws SQLException {
        // First check to make sure the id exists in the table
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT * FROM Store WHERE StoreID = %d", id));
        if (!result.next()) {
            throw new SQLException(String.format("No such store ID: %d.", id));
        }
        currentStoreId = id;
    }

    public Map<Integer, Integer> getCart(int onlineCustomerId) {
        return allCustomersCart.get(onlineCustomerId);
    }

    public int getOnlineCustomerId() {
        return onlineCustomerId;
    }

    public void setOnlineCustomerId(int id) throws SQLException {
        // First check to make sure the id exists in the table
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT * FROM FrequentCustomer WHERE CustomerID = %d", id));
        if (!result.next()) {
            throw new SQLException(String.format("No such frequent customer ID: %d.", id));
        }
        onlineCustomerId = id;
    }

    public int getAnonymousCustomerId() {
        return anonymousCustomerId;
    }

    public void setCurrentSearchPhrase(String phrase) {
        currentSearchPhrase = phrase;
    }

    public void setCurrentSQLStatement(String statement) {
        currentSQLStatement = statement;
    }

    public EditableOnlineField getCurrentField() {
        return currentField;
    }

    public void setCurrentField(EditableOnlineField field) {
        this.currentField = field;
    }

    public int getCurrentProductUpcCode() {
        return currentProductUpcCode;
    }

    public void setCurrentProductUpcCode(int upcCode) {
        currentProductUpcCode = upcCode;
    }

    public void setCurrentCategory(ProductType currentCategory) {
        this.currentCategory = currentCategory;
    }

    public List<List<String>> getCurrentSQLStatementResults() throws SQLException {
        List<List<String>> results = new ArrayList<>();

        Statement statement = dbConnection.createStatement();
        // Determine if the statement was a query or not, thus expects results
        if (statement.execute(currentSQLStatement)) {
            ResultSet result = statement.getResultSet();

            ResultSetMetaData metadata = result.getMetaData();
            int columnCount = metadata.getColumnCount();

            List<String> header = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                header.add(metadata.getColumnName(i));
            }
            results.add(header);

            while (result.next()) {
                List<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.add(result.getString(i));
                }
                results.add(row);
            }
        }

        return results;
    }

    public List<Integer> getBrowsingProducts() throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result;

        if (browseByCategory) {
            result = statement.executeQuery(String.format("WITH OnlineStoreInventory AS (SELECT * FROM StoreInventory WHERE StoreID = %d), SpecificProduct AS (SELECT * FROM Product NATURAL JOIN %s) SELECT * FROM OnlineStoreInventory INNER JOIN SpecificProduct ON OnlineStoreInventory.ProductID = SpecificProduct.ProductUPCCode;", getOnlineStoreId(), currentCategory.table));
        } else {
            result = statement.executeQuery(String.format("WITH OnlineStoreInventory AS (SELECT * FROM StoreInventory WHERE StoreID = %d) SELECT * FROM OnlineStoreInventory INNER JOIN Product ON OnlineStoreInventory.ProductID = Product.ProductUPCCode where LOWER(ProductName) LIKE LOWER('%%%s%%');", getOnlineStoreId(), currentSearchPhrase));
        }

        List<Integer> products = new ArrayList<>();
        while (result.next()) {
            products.add(result.getInt("ProductUPCCode"));
        }
        return products;
    }

    public String getProductName(int upcCode) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT ProductName FROM Product WHERE ProductUPCCode = %d", upcCode));
        result.next();
        return result.getString("ProductName");
    }

    public float getOnlineStorePrice(int upcCode) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT Price FROM StoreInventory WHERE StoreID = %d AND ProductID = %d", getOnlineStoreId(), upcCode));
        result.next();
        return result.getFloat("Price");
    }

    public float getStorePrice(int upcCode) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT Price FROM StoreInventory WHERE StoreID = %d AND ProductID = %d", currentStoreId, upcCode));
        result.next();
        return result.getFloat("Price");
    }

    // This is the quantity the store has, not the customer has in their cart. Use the cart HashMap for that
    public int getOnlineStoreQuantity(int upcCode) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT Quantity FROM StoreInventory WHERE StoreID = %d AND ProductID = %d", getOnlineStoreId(), upcCode));
        result.next();
        return result.getInt("Quantity");
    }

    private int getOnlineStoreId() throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery("SELECT StoreID FROM Store where StoreID NOT IN (SELECT StoreID from PhysicalStore)");
        result.next();
        return result.getInt("StoreID");
    }

    public void resetOnlineCustomerId() {
        onlineCustomerId = 0;
    }

    private int newID(String table, String primaryKey) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT MAX(%s) FROM %s", primaryKey, table));
        result.next();
        return result.getInt(String.format("MAX(%s)", primaryKey)) + 1;
    }

    public void setOnlineCustomerAddress(String street, String city, String state, int zip) throws SQLException {
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("UPDATE FrequentCustomer SET Street = '%s', City = '%s', State = '%s', Zip = %d WHERE CustomerID = %d", street, city, state, zip, onlineCustomerId));
    }

    public void setOnlineCustomerEmail(String email) throws SQLException {
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("UPDATE FrequentCustomer SET Email = '%s' WHERE CustomerID = %d", email, onlineCustomerId));
    }

    public void addOnlineCustomerPhone(int area, int prefix, int line) throws SQLException {
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("INSERT INTO FrequentCustomerPhone (CustomerId, AreaCode, Prefix, LineNumber) VALUES (%d, %d, %d, %d)", onlineCustomerId, area, prefix, line));
    }

    public List<String> getOnlineCustomerPhoneNumbers() throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT AreaCode, Prefix, LineNumber FROM FrequentCustomerPhone WHERE CustomerID = %d", onlineCustomerId));
        List<String> numbers = new ArrayList<>();
        while (result.next()) {
            numbers.add(String.format("%03d-%03d-%04d", result.getInt("AreaCode"), result.getInt("Prefix"), result.getInt("LineNumber")));
        }
        return numbers;
    }

    public int insertNewOnlineCustomer() throws SQLException {
        Statement statement = dbConnection.createStatement();
        int id = newID("FrequentCustomer", "CustomerID");
        statement.execute(String.format("INSERT INTO FrequentCustomer(CustomerID, CustomerName, Street, City, State, Zip, Points) values (%d, '', '', '', '', 0, 0)", id));
        return id;
    }

    public void insertCustomerSale() throws SQLException {
        int saleID = this.newID("CustomerSale", "SaleID");
        Instant instant = Instant.now();
        int timeStampSeconds = (int) instant.getEpochSecond();
        int storeID = this.getOnlineStoreId();
        int customerID = this.getOnlineCustomerId();
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("INSERT INTO CustomerSale(SaleID, SaleDate, PaymentMethod, StoreID, CustomerID) values (%d, %d, 'Credit', %d, %d)", saleID, timeStampSeconds, storeID, customerID));
    }

    public void insertCustomerSaleAnonymous() throws SQLException {
        int saleID = this.newID("CustomerSale", "SaleID");
        Instant instant = Instant.now();
        int timeStampSeconds = (int) instant.getEpochSecond();
        int storeID = this.getOnlineStoreId();
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("INSERT INTO CustomerSale(SaleID, SaleDate, PaymentMethod, StoreID, CustomerID) values (%d, %d, 'Credit', %d, %d)", saleID, timeStampSeconds, storeID, null));
    }

    public String getOnlineCustomerName() throws SQLException {
        return getOnlineCustomerField(EditableOnlineField.NAME);
    }

    public void setOnlineCustomerName(String name) throws SQLException {
        setOnlineCustomerField(EditableOnlineField.NAME, name);
    }

    public void setPoints() {
        usePoints = true;
    }

    public boolean usePoints() {
        return usePoints;
    }

    public void resetPoints() {
        usePoints = false;
    }

    public float getCash() {
        return this.cash;
    }

    public void setCash(float cash) {
        this.cash = cash;
    }

    public void incrementPoints() throws SQLException {
        float points = (float) getOnlineCustomerPoints() + (float) (getTotalPrice() * (5.0 / 100.0));
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("UPDATE FrequentCustomer SET points = %d WHERE customerID = %d", (int) points, onlineCustomerId));
    }

    public float getTotalPrice() {
        return this.totalPrice;
    }

    public void setTotalPrice(float totalPrice) {
        this.totalPrice = totalPrice;
    }

    public void setOnlineProductQuantity(int upcCode, int quantity) throws SQLException {
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("UPDATE StoreInventory SET quantity = %d WHERE productID = %d", quantity, upcCode));
    }

    public int getOnlineCustomerPoints() throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT Points FROM FrequentCustomer WHERE CustomerID = %d", onlineCustomerId));
        result.next();
        return result.getInt("Points");
    }

    public void setOnlineCustomerPoints(int quantity) throws SQLException {
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("UPDATE FrequentCustomer SET points = %d WHERE customerID = %d", quantity, onlineCustomerId));
    }

    public String getVendorName() throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT VendorName FROM Vendor WHERE VendorID = %d", vendorID));
        result.next();
        return result.getString("VendorName");
    }

    /**
     * @return the vendorID
     */
    public int getVendorID() {
        return vendorID;
    }

    /**
     * @param vendorID the vendorID to set
     */
    public void setVendorID(int vendorID) throws SQLException {
        // First check to make sure the id exists in the table
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT * FROM Vendor WHERE VendorID = %d", vendorID));
        if (!result.next()) {
            throw new SQLException(String.format("No such frequent customer ID: %d.", vendorID));
        }
        this.vendorID = vendorID;
    }

    public void setOrderToProcess(int order) {
        this.orderToProcess = order;
    }

    public void setOrderToDelete(int order) {
        this.orderToDelete = order;
    }

    public void deleteVendorOrder() throws SQLException {
        VendorOrder order = vendorOrders.get(this.orderToDelete);
        String query = String.format(DELETE_ORDER, Integer.parseInt(order.orderDetails.SaleID));
        Statement statement = dbConnection.createStatement();
        statement.execute(query);
    }

    public void processVendorOrder(Date arrivalDate) throws SQLException {
        Random r = new Random();
        int shipmentID = 100 + Math.abs(r.nextInt());
        int saleID = Integer.parseInt(vendorOrders.get(orderToProcess).orderDetails.SaleID);
        String createRelationship = String.format(CREATE_VENDOR_SALE_SHIPMENT, saleID, shipmentID);
        String createShipment = String.format(CREATE_SHIPMENT, shipmentID, arrivalDate.getTime() / 1000, " ", " ", " ", 5, " ", " ", " ", 10);
        Statement statement = dbConnection.createStatement();
        Statement statement2 = dbConnection.createStatement();
        statement.execute(createRelationship);
        statement2.execute(createShipment);
    }

    public List<VendorOrder> getInTransitOrders() throws SQLException {
        long timeNow = Instant.now().toEpochMilli() / 1000;
        Statement statement = dbConnection.createStatement();
        ResultSet orderDetails = statement.executeQuery(String.format(VENDOR_SALE_IN_TRANSIT, this.vendorID, timeNow));
        statement = dbConnection.createStatement();
        ResultSet productSummaries = statement.executeQuery(String.format(GET_VENDOR_SALE_PRODUCTS, this.vendorID));

        List<VendorOrder> orders = new ArrayList<>();
        productSummaries.next();
        while (orderDetails.next()) {
            orders.add(new VendorOrder(orderDetails, productSummaries));
        }
        this.vendorOrders = orders;
        return orders;
    }

    public List<VendorOrder> getAllVendorOrders() throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet orderDetails = statement.executeQuery(String.format(GET_VENDOR_SALE, this.vendorID));
        statement = dbConnection.createStatement();
        ResultSet productSummaries = statement.executeQuery(String.format(GET_VENDOR_SALE_PRODUCTS, this.vendorID));

        List<VendorOrder> orders = new ArrayList<>();
        productSummaries.next();
        while (orderDetails.next()) {
            orders.add(new VendorOrder(orderDetails, productSummaries));
        }
        this.vendorOrders = orders;
        return orders;
    }

    public List<VendorOrder> getUnProcessedOrders() throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet orderDetails = statement.executeQuery(String.format(GET_VENDOR_SALE_UNPROCESSED, this.vendorID));
        statement = dbConnection.createStatement();
        ResultSet productSummaries = statement.executeQuery(String.format(GET_VENDOR_SALE_PRODUCTS, this.vendorID));

        List<VendorOrder> orders = new ArrayList<>();
        productSummaries.next();
        while (orderDetails.next()) {
            orders.add(new VendorOrder(orderDetails, productSummaries));
        }
        this.vendorOrders = orders;
        return orders;
    }

    public String getOnlineCustomerField(EditableOnlineField field) throws SQLException {
        Statement statement = dbConnection.createStatement();
        ResultSet result = statement.executeQuery(String.format("SELECT %s FROM FrequentCustomer WHERE CustomerID = %d", field.column, onlineCustomerId));
        result.next();
        return result.getString(field.column);
    }

    public void setOnlineCustomerField(EditableOnlineField field, String value) throws SQLException {
        Statement statement = dbConnection.createStatement();
        statement.execute(String.format("UPDATE FrequentCustomer SET %s = '%s' WHERE CustomerID = %d", field.column, value, onlineCustomerId));
    }

    @Override
    public void close() throws IOException {
        try {
            dbConnection.close();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public enum ProductType {
        CLEANING("CleaningProduct", "Cleaning supplies"), DRUG("DrugProduct", "Medicine"), FOOD("FoodProduct", "Food"), FURNITURE("FurnitureProduct", "Furniture"), CLOTHING("ClothingProduct", "Clothing");
        private final String table;
        private final String toStr;

        ProductType(String table, String toStr) {
            this.table = table;
            this.toStr = toStr;
        }

        @Override
        public String toString() {
            return this.toStr;
        }
    }

    public enum EditableOnlineField {
        NAME("CustomerName", "([A-Za-z]+ [A-Za-z]+)", "Name"), STREET("Street", "([0-9A-Za-z ]+)"), CITY("City", "([A-Za-z ]+)"), STATE("State", "([A-Z]{2})"), ZIP("Zip", "(\\d+)"), EMAIL("Email", "(.+@.+)");
        private final String column;
        private final String toStr;
        private final String regex;

        EditableOnlineField(String column, String regex) {
            this.column = column;
            this.toStr = column;
            this.regex = regex;
        }

        EditableOnlineField(String column, String regex, String toStr) {
            this.column = column;
            this.regex = regex;
            this.toStr = toStr;
        }

        @Override
        public String toString() {
            return this.toStr;
        }

        public String getRegex() {
            return regex;
        }
    }

    public class VendorOrder {

        public OrderDetails orderDetails;
        public List<ProductSummary> products = new ArrayList<>();
        public int totalCost = 0;

        public VendorOrder(ResultSet orderDetails, ResultSet productSummaries) throws SQLException {
            this.orderDetails = new OrderDetails(orderDetails);
            do {
                ProductSummary currProduct = new ProductSummary(productSummaries);
                products.add(currProduct);
                totalCost += currProduct.totalAmount;
            } while (productSummaries.next() &&
                    Integer.parseInt(this.orderDetails.SaleID) == productSummaries.getInt("SaleID"));
        }
    }

    class OrderDetails {

        String SaleID, StoreID, Street, City, State, Zip, DateOfSale, DateOfDelivery;

        public OrderDetails(ResultSet rs) throws SQLException {
            SaleID = Integer.toString(rs.getInt("SALEID"));
            StoreID = Integer.toString(rs.getInt("StoreID"));
            Street = rs.getString("Street");
            City = rs.getString("City");
            State = rs.getString("State");
            Zip = Integer.toString(rs.getInt("Zip"));
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
            try {
                long saleDate = rs.getLong("DateOfSale");
                long deliveryDate = rs.getLong("ArrivalDate");
                DateOfSale = sdf.format(new Date(saleDate * 1000));
                DateOfDelivery = sdf.format(new Date(deliveryDate * 1000));
            } catch (Exception e) {

            }
        }
    }

    class ProductSummary {
        String ProductName, Quantity, Price;
        int totalAmount;

        public ProductSummary(ResultSet rs) throws SQLException {
            ProductName = rs.getString("ProductName");
            Quantity = Integer.toString(rs.getInt("Quantity"));
            Price = Integer.toString(rs.getInt("Price"));
            totalAmount = rs.getInt("SUM(PRICE * QUANTITY)");
        }
    }
}
