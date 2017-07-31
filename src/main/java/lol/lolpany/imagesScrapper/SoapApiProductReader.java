package lol.lolpany.imagesScrapper;

import com.google.code.magja.model.product.Product;
import com.google.code.magja.service.ServiceException;
import com.google.code.magja.service.product.ProductRemoteService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class SoapApiProductReader implements Runnable {

    private final static  Set<String> ATTRIBUTES = new HashSet<String>() {{
       add("sku");
       add("upc");
       add("name");
    }};

    final ProductRemoteService productService;
    final int from;
    final int to;
    final BlockingQueue<Product2> productQueue;

    public SoapApiProductReader(ProductRemoteService productService, int from, int n, BlockingQueue<Product2> productQueue) {
        this.productService = productService;
        this.from = from;
        this.to = from + n;
        this.productQueue = productQueue;
    }

    public void run() {
        for (int i = from; i < to; i++) {
            try {
                Product product = productService.getById(i,ATTRIBUTES );

//                Product existingProduct = productService.getById(i,ATTRIBUTES);
                    productQueue.put(new Product2(product.getId(),(String) product.getAttributes().get("sku"),
                            (String )product.getAttributes().get("upc"),
                            (String )product.getAttributes().get("name")));
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
