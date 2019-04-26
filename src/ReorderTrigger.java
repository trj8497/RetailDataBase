import org.h2.api.Trigger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

public class ReorderTrigger implements Trigger {
    @Override
    public void init(Connection connection, String s, String s1, String s2, boolean b, int i) throws SQLException {
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        // StoreInventory fields
        int storeId = (int) newRow[0];
        int productUpcCode = (int) newRow[1];
        int quantity = (int) newRow[2];
        int price = (int) newRow[3];
        int reorderThreshold = (int) newRow[4];

        // Check if there is already a reorder for this product that has not been unpacked
        Statement alreadyReorderedCheck = conn.createStatement();
        alreadyReorderedCheck.execute(String.format("SELECT COUNT(*) FROM  VendorSale INNER JOIN VendorSaleProduct ON VendorSale.SaleID = VendorSaleProduct.VendorSaleID WHERE StoreId = %d AND Unpacked = FALSE AND productUpcCode = %d;", storeId, productUpcCode));
        ResultSet res = alreadyReorderedCheck.getResultSet();
        res.next();
        int numNonUnpackedSalesWithProduct = res.getInt("COUNT(*)");

        // Check if store's quantity is lower than their reorder threshold
        if (newRow != null && quantity <= reorderThreshold && numNonUnpackedSalesWithProduct == 0) {
            // --Insert vendor sale--
            // Get the vendor that sells this product
            Statement vendor = conn.createStatement();
            vendor.executeQuery(String.format("SELECT * FROM Product NATURAL JOIN Brand NATURAL JOIN VendorBrand NATURAL JOIN Vendor WHERE ProductUPCCode = %d;", productUpcCode));
            ResultSet vendorRes = vendor.getResultSet();
            vendorRes.next();
            int vendorId = vendorRes.getInt("VendorID");

            // Current epoch timestamp
            Instant instant = Instant.now();
            long dateOfSale = instant.getEpochSecond();

            // Vendor sale primary key id
            Statement newID = conn.createStatement();
            ResultSet newIDRes = newID.executeQuery("SELECT MAX(SaleID) FROM VendorSale");
            newIDRes.next();
            int saleID = newIDRes.getInt("MAX(SaleID)") + 1;

            // Finally insert the sale (reorder)
            Statement sale = conn.createStatement();
            sale.execute(String.format("INSERT INTO VendorSale VALUES (%d, %d, %d, %d, FALSE)", saleID, vendorId, storeId, dateOfSale));

            // --Insert vendor sale product--
            // Order 50 more items (the store buys them from the vendor at a 10% discount off their own price)
            Statement saleProduct = conn.createStatement();
            sale.execute(String.format("INSERT INTO VendorSaleProduct VALUES (%d, %d, %d, 50)", saleID, productUpcCode, (int) (price * .9)));
        }
    }

    @Override
    public void close() throws SQLException {
    }

    @Override
    public void remove() throws SQLException {
    }
}
