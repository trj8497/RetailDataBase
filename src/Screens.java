import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Screens {
    public static final Screen.ScreenGenerator home = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Retail Center\n");
            screen.addOption("Administrator", admin_home, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Online Customer", onlineCustomer_splash, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Vendor", vendor_splash, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Unpack Arrived Shipments", unpack_home, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Checkout Register", register_enterStoreId, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.add("\nChoose an application: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator vendor_splash = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(\\d+)$"), vendor_home, (matchedInput, dataToUpdate) -> {
                data.setVendorID(Integer.parseInt(matchedInput.group(1)));
            });
            screen.add("Welcome, enter vendor ID: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator vendor_home = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            String name = data.getVendorName();
            int id = data.getVendorID();
            screen.add(String.format("Welcome %s, ID: %d.\n", name, id));
            screen.addOption("View unshipped order requests", vendor_unprocessed, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("View orders in transit", vendor_in_transit, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("View All Orders", vendor_all_orders, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Log out", vendor_splash, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.add("\nChoose an option: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator vendor_in_transit = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Orders in transit to destination:\n");
            List<Data.VendorOrder> orders = data.getInTransitOrders();
            for (Data.VendorOrder order : orders) {
                Data.OrderDetails details = order.orderDetails;
                int totalPrice = order.totalCost;

                String orderSummaryString = String.format(
                        "Sale ID: %s\nFor Store: %s Located at %s %s, %s %s\nSale Date: %s\nDelivery Date: %s\n",
                        details.SaleID, details.StoreID, details.Street,
                        details.City, details.State, details.Zip,
                        details.DateOfSale, details.DateOfDelivery);

                String productSummariesString = "\nSummary Of Products Purchased:\n";
                for (Data.ProductSummary product : order.products) {
                    productSummariesString += String.format("Product: %s\nPurchased %s at $%s each\n",
                            product.ProductName, product.Quantity, product.Price);
                }

                String totalPriceString = String.format("\nTotal Price: $%.2f\n", (float) totalPrice);
                final int i = orders.indexOf(order);
                screen.addOption(orderSummaryString + productSummariesString + totalPriceString,
                        vendor_in_transit, (dataToUpdate) -> {
                            dataToUpdate.setOrderToDelete(i);
                            dataToUpdate.deleteVendorOrder();
                        });
            }
            screen.add("Select an order to cancel it, or alternatively,\n");
            screen.addOption("Return Home", vendor_home, (dataToUpdate -> {
            }));
            screen.add("\nPlease choose an option: ");

            return screen;
        }
    };
    public static final Screen.ScreenGenerator vendor_all_orders = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^.*$"), vendor_home, (matchedInput, dataToUpdate) -> {

            });
            screen.add("All orders:\n");
            List<Data.VendorOrder> orders = data.getAllVendorOrders();
            for (Data.VendorOrder order : orders) {
                Data.OrderDetails details = order.orderDetails;
                int totalPrice = order.totalCost;

                String orderSummaryString = String.format(
                        "Sale ID: %s\nFor Store: %s Located at %s %s, %s %s\nSale Date: %s\nDelivery Date: %s\n",
                        details.SaleID, details.StoreID, details.Street,
                        details.City, details.State, details.Zip,
                        details.DateOfSale, details.DateOfDelivery);

                String productSummariesString = "\nSummary Of Products Purchased:\n";
                for (Data.ProductSummary product : order.products) {
                    productSummariesString += String.format("Product: %s\nPurchased %s at $%s each\n",
                            product.ProductName, product.Quantity, product.Price);
                }

                String totalPriceString = String.format("\nTotal Price: $%.2f\n", (float) totalPrice);

                screen.add(orderSummaryString + productSummariesString + totalPriceString);
            }
            screen.add("Press enter to return: ");

            return screen;
        }
    };
    public static final Screen.ScreenGenerator vendor_unprocessed = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Orders Waitng to Be Processed:\n");
            List<Data.VendorOrder> orders = data.getUnProcessedOrders();
            for (Data.VendorOrder order : orders) {
                Data.OrderDetails details = order.orderDetails;
                int totalPrice = order.totalCost;

                String orderSummaryString = String.format(
                        "Sale ID: %s\nFor Store: %s Located at %s %s, %s %s\n",
                        details.SaleID, details.StoreID, details.Street,
                        details.City, details.State, details.Zip,
                        details.DateOfSale, details.DateOfDelivery);

                String productSummariesString = "\nSummary Of Products Purchased:\n";
                for (Data.ProductSummary product : order.products) {
                    productSummariesString += String.format("Product: %s\nPurchased %s at $%s each\n",
                            product.ProductName, product.Quantity, product.Price);
                }

                String totalPriceString = String.format("\nTotal Price: $%.2f\n", (float) totalPrice);
                final int i = orders.indexOf(order);
                screen.addOption(orderSummaryString + productSummariesString + totalPriceString,
                        vendor_unprocessed_decision, (dataToUpdate) -> {
                            data.setOrderToProcess(i);
                            data.setOrderToDelete(i);
                        });
            }

            screen.add("Select an order to interact with, or alternatively,\n");
            screen.addOption("Return Home", vendor_home, (dataToUpdate -> {
            }));

            screen.add("\nPlease choose an option: ");

            return screen;
        }
    };

    public static final Screen.ScreenGenerator vendor_unprocessed_decision = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("What would you like to do with this order?\n");
            screen.addOption("Ship", vendor_unprocessed_enter_date, (dataToUpdate) -> {
            });
            screen.addOption("Reject", vendor_unprocessed, (dataToUpdate) -> {
                dataToUpdate.deleteVendorOrder();
            });
            screen.add("\nPlease choose an option: ");

            return screen;
        }
    };

    public static final Screen.ScreenGenerator vendor_unprocessed_enter_date = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(\\d+)/(\\d+)/(\\d+)$"), vendor_unprocessed, (matchedInput, dataToUpdate) -> {
                String date = matchedInput.group(1) + "/" + matchedInput.group(2) + "/" + matchedInput.group(3);
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    data.processVendorOrder(sdf.parse(date));
                } catch (Exception e) {
                    System.out.println(e);
                }
                ;
            });
            screen.add("Enter date of Arrival: (mm/dd/yyyy) ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator unpack_home = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(\\d+)$"), unpack_arrived, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setCurrentStoreId(Integer.parseInt(matchedInput.group(1)));
            });
            screen.add("Enter store ID: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator unpack_arrived = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("The following shipments have arrived:\n");

            // Show all shipments that have arrived and have not been unpacked yet
            List<Integer> sales = data.getArrivedPackedSales();
            for (Integer saleID : sales) {
                screen.add(String.format("Package %d:", saleID));
                List<String> products = data.getVendorSaleProducts(saleID);
                for (String prod : products) {
                    screen.add(prod);
                }
                screen.add("");
            }

            screen.addOption("Unpack all", unpack_arrived, (dataToUpdate) -> {
                for (Integer saleID : sales) {
                    dataToUpdate.unpackVendorSale(saleID);
                }
            });
            screen.addOption("Change stores", unpack_home, (dataToUpdate) -> {
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_splash = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Welcome,\n");
            screen.addOption("New customer sign-up", onlineCustomer_name, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Existing customer login", onlineCustomer_id, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_name = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(Data.EditableOnlineField.NAME.getRegex()), onlineCustomer_address, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setOnlineCustomerId(dataToUpdate.insertNewOnlineCustomer());
                dataToUpdate.setOnlineCustomerName(matchedInput.group(1));
            });
            screen.add("Enter first and last name (e.g. Bob Evans): ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator admin_home = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            List<List<String>> results = data.getCurrentSQLStatementResults();
            InputScreen screen = new InputScreen(data, Pattern.compile("(.*)"), admin_home, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setCurrentSQLStatement(matchedInput.group(1));
            });
            if (results.size() != 0)
                screen.add("");
            for (List<String> row : results) {
                screen.add(String.join(", ", row));
            }
            if (results.size() != 0)
                screen.add("");
            screen.add("Enter SQL statement: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_address = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(String.format("^%s, %s, %s, %s$", Data.EditableOnlineField.STREET.getRegex(), Data.EditableOnlineField.CITY.getRegex(), Data.EditableOnlineField.STATE.getRegex(), Data.EditableOnlineField.ZIP.getRegex())), onlineCustomer_email, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setOnlineCustomerAddress(matchedInput.group(1), matchedInput.group(2), matchedInput.group(3), Integer.parseInt(matchedInput.group(4)));
            });
            screen.add("Enter address in format <Street, City, State, ZIP> (e.g. 907 William Street, Lake Villa, IL, 60046): ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_email = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(Data.EditableOnlineField.EMAIL.getRegex()), onlineCustomer_chooseAddPhone, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setOnlineCustomerEmail(matchedInput.group(1));
            });
            screen.add("Enter email address: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_chooseAddPhone = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("You may choose to add any number of phone numbers.\n");
            screen.addOption("Add phone number", onlineCustomer_phone, (dataToUpdate) -> {
                // No data to update
            });
            screen.addOption("Finish", onlineCustomer_home, (dataToUpdate) -> {
                // No data to update
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_phone = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(Data.PHONE_REGEX), onlineCustomer_chooseAddPhone, (matchedInput, dataToUpdate) -> {
                dataToUpdate.addOnlineCustomerPhone(Integer.parseInt(matchedInput.group(1)), Integer.parseInt(matchedInput.group(2)), Integer.parseInt(matchedInput.group(3)));
            });
            screen.add("Enter phone number (e.g. 123-456-7890): ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator onlineCustomer_profileAddPhone = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(Data.PHONE_REGEX), onlineCustomer_profile, (matchedInput, dataToUpdate) -> {
                dataToUpdate.addOnlineCustomerPhone(Integer.parseInt(matchedInput.group(1)), Integer.parseInt(matchedInput.group(2)), Integer.parseInt(matchedInput.group(3)));
            });
            screen.add("Enter phone number (e.g. 123-456-7890): ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator scanProduct_Checkout = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(\\d+)$"), frequentShopper_home, (matchedInput, dataToUpdate) -> {
                int upcCode = Integer.parseInt(matchedInput.group(1));
                dataToUpdate.checkIfStoreStocksProduct(upcCode);
                dataToUpdate.addToCart(data.getOnlineCustomerId(), upcCode, 1);
            });
            screen.add("Enter UPC code: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator changeReturned = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(".*"), register_chooseSaleType, (matchedInput, dataToUpdate) -> {
                dataToUpdate.incrementPoints();
                dataToUpdate.removeCustomerFromCart(data.getOnlineCustomerId());
            });
            float change = data.getCash() - data.getTotalPrice();
            screen.add(String.format("Change: $%.2f", change));
            screen.add("Press enter to finish checkout.");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkout_Cash = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("(^\\d+\\.?\\d*$)"), changeReturned, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setCash(Float.parseFloat(matchedInput.group(1)));
            });
            screen.add("Enter given cash: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator creditPaymentSuccessful = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(".*"), register_chooseSaleType, (matchedInput, dataToUpdate) -> {
                dataToUpdate.incrementPoints();
                dataToUpdate.removeCustomerFromCart(data.getOnlineCustomerId());
            });
            screen.add("Payment successful, press enter to checkout: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkout_Credit = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("(^[1-9]\\d*$)"), creditPaymentSuccessful, ((matchedInput, dataToUpdate) -> {
            }));
            screen.add("Enter card number (16-digits): ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkout_Check = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(".*"), register_chooseSaleType, (matchedInput, dataToUpdate) -> {
                dataToUpdate.incrementPoints();
                dataToUpdate.removeCustomerFromCart(data.getOnlineCustomerId());
            });
            screen.add("Place check in safe, press enter to finish checkout: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator PaymentDetails = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            Map<Integer, Integer> cart = data.getCart(data.getOnlineCustomerId());
            float calcPrice = 0;
            for (Integer productID : cart.keySet()) {
                float price = data.getStorePrice(productID);
                int quantity = cart.get(productID);
                calcPrice += price * quantity;
            }

            if (data.usePoints()) {
                calcPrice -= (0.2) * data.getOnlineCustomerPoints();

                if (calcPrice < 0) {
                    calcPrice = 0;
                }
                data.setOnlineCustomerPoints(0);
                data.resetPoints();
            }


            screen.add(String.format("The total price is $%.2f", calcPrice));

            data.setTotalPrice(calcPrice);

            screen.add("Select payment Method.\n");
            screen.addOption("Cash", checkout_Cash, dataToUpdate -> {
            });
            screen.addOption("Credit", checkout_Credit, dataToUpdate -> {
            });
            screen.addOption("Check", checkout_Check, dataToUpdate -> {
            });

            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkoutCart_Points_InStore = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Use points towards purchase?\n");
            screen.addOption("Yes", PaymentDetails, dataToUpdate -> {
                dataToUpdate.setPoints();
            });
            screen.addOption("No", PaymentDetails, dataToUpdate -> {
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator changeQuantity_FreqShopper = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("(^[1-9]\\d*$)"), viewCart_InStore, ((matchedInput, dataToUpdate) -> {
                dataToUpdate.setCartQuantity(data.getOnlineCustomerId(), data.getCurrentProductUpcCode(), Integer.parseInt(matchedInput.group(1)));
            }));
            screen.add("Enter new quantity: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator removeOrChangeQty_FreqShopper = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            int upcCode = data.getCurrentProductUpcCode();
            String productName = data.getProductName(upcCode);
            //fix
            float price = data.getStorePrice(upcCode);
            int quantity = data.getCart(data.getOnlineCustomerId()).get(upcCode);

            screen.add("Product:");
            screen.add(String.format("%-50s $%-50.2f x%d\n", productName, price, quantity));
            screen.addOption("Remove", viewCart_InStore, dataToUpdate -> {
                dataToUpdate.removeProductFromCart(data.getOnlineCustomerId(), upcCode);
            });
            screen.addOption("Change quantity", changeQuantity_FreqShopper, dataToUpdate -> {
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator viewCart_InStore = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            Map<Integer, Integer> cart = data.getCart(data.getOnlineCustomerId());
            int count = cart.size();
            screen.add(String.format("You have %d distinct products currently in your cart:\n", count));
            screen.add(String.format("%-53s %-50s %s", "Product Name", "Price", "Quantity"));
            float calcPrice = 0;
            for (Integer productID : cart.keySet()) {
                String productName = data.getProductName(productID);
                float price = data.getStorePrice(productID);
                int quantity = cart.get(productID);
                calcPrice += price * quantity;
                screen.addOption(String.format("%-50s $%-50.2f x%d", productName, price, quantity), removeOrChangeQty_FreqShopper, (dataToUpdate -> {
                    dataToUpdate.setCurrentProductUpcCode(productID);
                }));
            }
            screen.add(String.format("\nTotal price: $%.2f", calcPrice));
            screen.add("\nSelect a product from the cart, or alternatively,\n");
            screen.addOption("Checkout", checkoutCart_Points_InStore, (dataToUpdate -> {
            }));
            screen.addOption("Return home", frequentShopper_home, (dataToUpdate -> {
            }));

            return screen;
        }
    };

    public static final Screen.ScreenGenerator frequentShopper_home = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add(String.format("Welcome %s!", data.getOnlineCustomerName()));
            screen.add(String.format("Customer ID: %d", data.getOnlineCustomerId()));
            screen.add(String.format("Points: %d\n", data.getOnlineCustomerPoints()));

            data.addCartToCarts(data.getOnlineCustomerId());

            screen.addOption("View cart / checkout", viewCart_InStore, dataToUpdate -> {
            });
            screen.addOption("Scan product to cart", scanProduct_Checkout, dataToUpdate -> {
            });
            screen.addOption("View Customer Information", register_profile, dataToUpdate -> {
            });
            screen.addOption("Cancel checkout", register_chooseSaleType, dataToUpdate -> {
                dataToUpdate.removeCustomerFromCart(data.getOnlineCustomerId());
            });

            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator frequentShopper_id = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(\\d+)$"), frequentShopper_home, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setOnlineCustomerId(Integer.parseInt(matchedInput.group(1)));
            });
            screen.add("Enter Frequent shopper ID: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator scanProductAnonymous_Checkout = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(\\d+)$"), anonymousShopper_sale, (matchedInput, dataToUpdate) -> {
                int upcCode = Integer.parseInt(matchedInput.group(1));
                dataToUpdate.checkIfStoreStocksProduct(upcCode);
                dataToUpdate.addToCart(data.getAnonymousCustomerId(), upcCode, 1);
            });
            screen.add("Enter UPC code: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator changeReturnedAnonymous = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(".*"), register_chooseSaleType, (matchedInput, dataToUpdate) -> {
                dataToUpdate.removeCustomerFromCart(data.getAnonymousCustomerId());
            });
            float change = data.getCash() - data.getTotalPrice();
            screen.add(String.format("Change: $%.2f", change));
            screen.add("Press enter to finish checkout: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkoutAnonymous_Cash = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("(^\\d+\\.?\\d*$)"), changeReturnedAnonymous, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setCash(Float.parseFloat(matchedInput.group(1)));
            });
            screen.add("Enter given cash: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator creditPaymentSuccessfulAnonymous = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(".*"), register_chooseSaleType, (matchedInput, dataToUpdate) -> {
                dataToUpdate.removeCustomerFromCart(data.getAnonymousCustomerId());
            });
            screen.add("Payment successful, press enter to checkout: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkoutAnonymous_Credit = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("(^[1-9]\\d*$)"), creditPaymentSuccessfulAnonymous, ((matchedInput, dataToUpdate) -> {
            }));
            screen.add("Enter card number (16-digits): ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkoutAnonymous_Check = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(".*"), register_chooseSaleType, (matchedInput, dataToUpdate) -> {
                dataToUpdate.removeCustomerFromCart(data.getAnonymousCustomerId());
            });
            screen.add("Place check in safe, press enter to finish checkout: ");
            return screen;
        }
    };


    public static final Screen.ScreenGenerator PaymentDetails_Anonymous = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            Map<Integer, Integer> cart = data.getCart(data.getAnonymousCustomerId());
            float calcPrice = 0;
            for (Integer productID : cart.keySet()) {
                float price = data.getStorePrice(productID);
                int quantity = cart.get(productID);
                calcPrice += price * quantity;
            }

            data.setTotalPrice(calcPrice);

            screen.add(String.format("The total price is $%.2f", calcPrice));
            screen.add("Select payment Method.\n");
            screen.addOption("Cash", checkoutAnonymous_Cash, dataToUpdate -> {
            });
            screen.addOption("Credit", checkoutAnonymous_Credit, dataToUpdate -> {
            });
            screen.addOption("Check", checkoutAnonymous_Check, dataToUpdate -> {
            });

            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator changeQuantityAnonymous = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("(^[1-9]\\d*$)"), viewCartAnonymous_InStore, ((matchedInput, dataToUpdate) -> {
                dataToUpdate.setCartQuantity(data.getAnonymousCustomerId(), data.getCurrentProductUpcCode(), Integer.parseInt(matchedInput.group(1)));
            }));
            screen.add("Enter new quantity: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator removeOrChangeQty_Anonymous = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            int upcCode = data.getCurrentProductUpcCode();
            String productName = data.getProductName(upcCode);
            //fix
            float price = data.getStorePrice(upcCode);
            int quantity = data.getCart(data.getAnonymousCustomerId()).get(upcCode);

            screen.add("Product:");
            screen.add(String.format("%-50s $%-50.2f x%d\n", productName, price, quantity));
            screen.addOption("Remove", viewCartAnonymous_InStore, dataToUpdate -> {
                dataToUpdate.removeProductFromCart(data.getAnonymousCustomerId(), upcCode);
            });
            screen.addOption("Change quantity", changeQuantityAnonymous, dataToUpdate -> {
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator viewCartAnonymous_InStore = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            Map<Integer, Integer> cart = data.getCart(data.getAnonymousCustomerId());
            int count = cart.size();
            screen.add(String.format("You have %d distinct products currently in your cart:\n", count));
            screen.add(String.format("%-53s %-50s %s", "Product Name", "Price", "Quantity"));
            float calcPrice = 0;
            for (Integer productID : cart.keySet()) {
                String productName = data.getProductName(productID);
                float price = data.getStorePrice(productID);
                int quantity = cart.get(productID);
                calcPrice += price * quantity;
                screen.addOption(String.format("%-50s $%-50.2f x%d", productName, price, quantity), removeOrChangeQty_Anonymous, (dataToUpdate -> {
                    dataToUpdate.setCurrentProductUpcCode(productID);
                }));
            }
            screen.add(String.format("\nTotal price: $%.2f", calcPrice));
            screen.add("\nSelect a product from the cart, or alternatively,\n");
            screen.addOption("Checkout", PaymentDetails_Anonymous, (dataToUpdate -> {
            }));
            screen.addOption("Return home", anonymousShopper_sale, (dataToUpdate -> {
            }));
            screen.add("\nPlease choose an option: ");

            return screen;
        }
    };

    public static final Screen.ScreenGenerator anonymousShopper_sale = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);

            data.addCartToCarts(data.getAnonymousCustomerId());

            screen.addOption("View cart / checkout", viewCartAnonymous_InStore, dataToUpdate -> {
            });
            screen.addOption("Scan product to cart", scanProductAnonymous_Checkout, dataToUpdate -> {
            });
            screen.addOption("Cancel checkout", register_chooseSaleType, dataToUpdate -> {
                data.removeCustomerFromCart(data.getAnonymousCustomerId());
            });

            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator register_chooseSaleType = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Welcome,\n");

            screen.addOption("Create frequent shopper sale", frequentShopper_id, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Create anonymous sale", anonymousShopper_sale, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator register_enterStoreId = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(\\d+)$"), register_chooseSaleType, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setCurrentStoreId(Integer.parseInt(matchedInput.group(1)));
            });
            screen.add("Enter your store ID: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_id = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(\\d+)$"), onlineCustomer_home, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setOnlineCustomerId(Integer.parseInt(matchedInput.group(1)));
            });
            screen.add("Enter your customer ID: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkoutCart_Online = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(".*"), onlineCustomer_home, ((matchedInput, dataToUpdate) -> {
                data.incrementPoints();
            }));
            screen.add("Thank you for shopping with us!");
            screen.add(String.format("%s, your order has been placed and will ship to your address on file.\n", data.getOnlineCustomerName()));
            screen.add("Order Summary:");
            Map<Integer, Integer> cart = data.getCart(data.getOnlineCustomerId());
            screen.add(String.format("%-50s %-50s %s", "Product Name", "Price", "Quantity"));
            float calcPrice = 0;
            for (Integer productID : cart.keySet()) {
                String productName = data.getProductName(productID);
                float price = data.getOnlineStorePrice(productID);
                int quantity = cart.get(productID);
                calcPrice += price * quantity;
                screen.add(String.format("%-50s $%-50.2f x%d", productName, price, quantity));
                int storeQuantity = data.getOnlineStoreQuantity(productID) - quantity;
                data.setOnlineProductQuantity(productID, storeQuantity);
            }

            if (data.usePoints()) {
                calcPrice -= (0.2) * data.getOnlineCustomerPoints();

                if (calcPrice < 0) {
                    calcPrice = 0;
                }

                data.setOnlineCustomerPoints(0);
                data.resetPoints();
            }

            screen.add(String.format("\nTotal price: $%.2f", calcPrice));

            data.setTotalPrice(calcPrice);

            data.insertCustomerSale();
            data.removeCustomerFromCart(data.getOnlineCustomerId());
            screen.add("Press enter to return home: ");

            return screen;
        }
    };

    public static final Screen.ScreenGenerator cardDetails = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("(^[1-9]\\d*$)"), checkoutCart_Online, ((matchedInput, dataToUpdate) -> {
            }));
            screen.add("Enter card number (16-digits): ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator checkoutCart_Points = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Use points towards purchase?\n");
            screen.addOption("Yes", cardDetails, dataToUpdate -> {
                dataToUpdate.setPoints();
            });
            screen.addOption("No", cardDetails, dataToUpdate -> {
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator changeQuantity = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("(^[1-9]\\d*$)"), viewCart_Online, ((matchedInput, dataToUpdate) -> {
                dataToUpdate.setCartQuantity(data.getOnlineCustomerId(), data.getCurrentProductUpcCode(), Integer.parseInt(matchedInput.group(1)));
            }));
            screen.add("Enter new quantity: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator removeOrChangeQty_Online = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            int upcCode = data.getCurrentProductUpcCode();
            String productName = data.getProductName(upcCode);
            float price = data.getOnlineStorePrice(upcCode);
            int quantity = data.getCart(data.getOnlineCustomerId()).get(upcCode);

            screen.add("Product:");
            screen.add(String.format("%-50s $%-50.2f x%d\n", productName, price, quantity));
            screen.addOption("Remove", viewCart_Online, dataToUpdate -> {
                dataToUpdate.removeProductFromCart(data.getOnlineCustomerId(), upcCode);
            });
            screen.addOption("Change quantity", changeQuantity, dataToUpdate -> {
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator viewCart_Online = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            Map<Integer, Integer> cart = data.getCart(data.getOnlineCustomerId());
            int count = cart.size();
            screen.add(String.format("You have %d distinct products currently in your cart:\n", count));
            screen.add(String.format("%-53s %-50s %s", "Product Name", "Price", "Quantity"));
            float calcPrice = 0;
            for (Integer productID : cart.keySet()) {
                String productName = data.getProductName(productID);
                float price = data.getOnlineStorePrice(productID);
                int quantity = cart.get(productID);
                calcPrice += price * quantity;
                screen.addOption(String.format("%-50s $%-50.2f x%d", productName, price, quantity), removeOrChangeQty_Online, (dataToUpdate -> {
                    dataToUpdate.setCurrentProductUpcCode(productID);
                }));
            }
            screen.add(String.format("\nTotal price: $%.2f", calcPrice));
            screen.add("\nSelect a product from the cart, or alternatively,\n");
            screen.addOption("Checkout", checkoutCart_Points, (dataToUpdate -> {
            }));
            screen.addOption("Return home", onlineCustomer_home, (dataToUpdate -> {
            }));
            screen.add("\nPlease choose an option: ");

            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_home = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add(String.format("Hello, %s, ID: %s. You have %d frequent shopper points.\n", data.getOnlineCustomerName(), data.getOnlineCustomerId(), data.getOnlineCustomerPoints()));
            data.addCartToCarts(data.getOnlineCustomerId());

            screen.addOption("View cart / checkout", viewCart_Online, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("View product categories", onlineCustomer_productCategory, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Search for product", onlineCustomer_search, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("View customer information", onlineCustomer_profile, (dataToUpdate) -> {
                // If applicable, update data specific to if this option is chosen
            });
            screen.addOption("Log out", onlineCustomer_splash, (dataToUpdate) -> {
                dataToUpdate.resetOnlineCustomerId();
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator onlineCustomer_profile = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Customer information:\n");

            screen.add(String.format("%-18s %50s", "Customer ID:", data.getOnlineCustomerId()));
            screen.add(String.format("%-18s %50s", "Points:", data.getOnlineCustomerPoints()));
            for (Data.EditableOnlineField field : Data.EditableOnlineField.values()) {
                screen.addOption(String.format("%-15s %50s", field + ":", data.getOnlineCustomerField(field)), onlineCustomer_changeField, dataToUpdate -> {
                    dataToUpdate.setCurrentField(field);
                });
            }
            screen.add("Phone numbers:");
            for (String number : data.getOnlineCustomerPhoneNumbers()) {
                screen.add(number);
            }
            screen.add("\nSelect a field to modify, or alternatively,\n");
            screen.addOption("Add phone number", onlineCustomer_profileAddPhone, dataToUpdate -> {
            });
            screen.addOption("Return home", onlineCustomer_home, dataToUpdate -> {
            });
            screen.add("\nPlease choose an option: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator register_profile = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(".*"), frequentShopper_home, (matchedInput, dataToUpdate) -> {
            });

            screen.add("Customer information:\n");

            screen.add(String.format("%-15s %50s", "Customer ID:", data.getOnlineCustomerId()));
            screen.add(String.format("%-15s %50s", "Points:", data.getOnlineCustomerPoints()));
            for (Data.EditableOnlineField field : Data.EditableOnlineField.values()) {
                screen.add(String.format("%-15s %50s", field + ":", data.getOnlineCustomerField(field)));
            }
            screen.add("Phone numbers:");
            for (String number : data.getOnlineCustomerPhoneNumbers()) {
                screen.add(number);
            }
            screen.add("Press enter to return home: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator onlineCustomer_changeField = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile(String.format("^%s$", data.getCurrentField().getRegex())), onlineCustomer_profile, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setOnlineCustomerField(dataToUpdate.getCurrentField(), matchedInput.group(1));
            });
            screen.add("Enter new value using original format: ");
            return screen;
        }
    };
    public static final Screen.ScreenGenerator onlineCustomer_productCategory = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("The following product categories are available:\n");

            for (Data.ProductType type : Data.ProductType.values()) {
                screen.addOption(type.toString(), onlineCustomer_browsing, (dataToUpdate) -> {
                    dataToUpdate.setCurrentCategory(type);
                    dataToUpdate.setBrowseByCategory(true);
                });
            }
            screen.add("\nChoose the category you wish to view: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_search = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^(.*)$"), onlineCustomer_browsing, (matchedInput, dataToUpdate) -> {
                dataToUpdate.setCurrentSearchPhrase(matchedInput.group(1));
                dataToUpdate.setBrowseByCategory(false);
            });
            screen.add("Enter product search phrase: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_browsing = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            OptionScreen screen = new OptionScreen(data);
            screen.add("Available products:\n");
            List<Integer> products = data.getBrowsingProducts();
            for (Integer product : products) {
                screen.addOption(String.format("%-50s $%-15.2f x%d", data.getProductName(product), data.getOnlineStorePrice(product), data.getOnlineStoreQuantity(product)), onlineCustomer_addCartQuantity, (dataToUpdate) -> {
                    dataToUpdate.setCurrentProductUpcCode(product);
                });
            }
            screen.add("\nSelect a product to add to your cart, or alternatively,\n");
            screen.addOption("Search for product", onlineCustomer_search, (dataToUpdate) -> {
            });
            screen.addOption("View product categories", onlineCustomer_productCategory, (dataToUpdate) -> {
            });
            screen.addOption("Return home", onlineCustomer_home, (dataToUpdate) -> {
            });
            screen.add("\nChoose an option: ");
            return screen;
        }
    };

    public static final Screen.ScreenGenerator onlineCustomer_addCartQuantity = new Screen.ScreenGenerator() {
        @Override
        public Screen generate(Data data) throws SQLException {
            InputScreen screen = new InputScreen(data, Pattern.compile("^([1-9]\\d*)$"), onlineCustomer_browsing, (matchedInput, dataToUpdate) -> {
                dataToUpdate.addToCart(dataToUpdate.getOnlineCustomerId(), dataToUpdate.getCurrentProductUpcCode(), Integer.parseInt(matchedInput.group(1)));
            });
            screen.add("Enter quantity (greater than 0): ");
            return screen;
        }
    };

}
