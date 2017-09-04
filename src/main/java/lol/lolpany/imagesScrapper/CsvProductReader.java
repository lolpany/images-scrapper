package lol.lolpany.imagesScrapper;

import java.io.File;
import java.sql.*;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import static lol.lolpany.imagesScrapper.Product2.END_QUEUE;

public class CsvProductReader implements Runnable {

    final Connection connection;
    final long from;
    final long to;
    final BlockingQueue<Product2> productQueue;
    final String url;
    final String username;
    final String password;
    final int downloaders;
    final String inputFile;

    public CsvProductReader(Connection connection, long from, long n, BlockingQueue<Product2> productQueue, String url, String username, String password, int downloaders, String inputFile) {
        this.connection = connection;
        this.from = from;
        this.url = url;
        this.username = username;
        this.password = password;
        this.inputFile = inputFile;
        this.to = from + n;
        this.productQueue = productQueue;
        this.downloaders= downloaders;
    }

    public void run() {
        try {
            Scanner scanner = new Scanner(new File(inputFile));
            int i = 0;
            while (scanner.hasNext() && i < from) {
                scanner.nextLine();
                i++;
            }
            while (scanner.hasNext()) {
                try {
                String[] productAttributes = scanner.nextLine().split("\t", 5);
                int productId = Integer.parseInt(productAttributes[0]);
                    System.out.println(productId);
                    i++;
                if (i > to) {
                    break;
                }
                productQueue.put(new Product2(productId, productAttributes[1], productAttributes[2],
                        productAttributes[2]));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
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
