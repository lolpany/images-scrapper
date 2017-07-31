package lol.lolpany.imagesScrapper;

import java.sql.*;
import java.util.concurrent.BlockingQueue;

import static lol.lolpany.imagesScrapper.Product2.END_QUEUE;

public class ProductReader implements Runnable {

    final Connection connection;
    final long from;
    final long to;
    final BlockingQueue<Product2> productQueue;
    final String url;
    final String username;
    final String password;
    final int downloaders;

    public ProductReader(Connection connection, long from, long n, BlockingQueue<Product2> productQueue, String url, String username, String password, int downloaders) {
        this.connection = connection;
        this.from = from;
        this.url = url;
        this.username = username;
        this.password = password;
        this.downloaders = downloaders;
        this.to = from + n;
        this.productQueue = productQueue;
    }

    public void run() {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            try (PreparedStatement ps = connection.prepareStatement("" +
                    "select product.entity_id as id,product.sku as sku , image.value as image_path, upc.value as upc_value, name.value as name_value\n" +
                    "     from (select entity_id, sku from catalog_product_entity where entity_type_id = 4 and entity_id > " +from+ " and entity_id < " +to+" order by entity_id ) product\n" +
                    "   left join catalog_product_entity_varchar image on (image.entity_id = product.entity_id  and image.attribute_id = 85)\n" +
                    "   left join catalog_product_entity_varchar upc  on (product.entity_id = upc.entity_id and upc.attribute_id = 132)\n" +
                    "             left join catalog_product_entity_varchar name  on (product.entity_id = name.entity_id and name.attribute_id = 71)")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        productQueue.put(new Product2(rs.getLong(1), rs.getString(2), rs.getString(4), rs.getString(5)));
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            for (int i =0; i< downloaders; i++) {
                productQueue.put(END_QUEUE);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
