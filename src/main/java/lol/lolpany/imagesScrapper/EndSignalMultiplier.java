package lol.lolpany.imagesScrapper;

import java.util.concurrent.BlockingQueue;

import static lol.lolpany.imagesScrapper.Product2.END_QUEUE;

public class EndSignalMultiplier implements Runnable {
    private final BlockingQueue<Product2> inputQueue;
    private final BlockingQueue<Product2> outputQueue;
    private final int consumerNodesNumber;

    public EndSignalMultiplier(BlockingQueue<Product2> inputQueue, BlockingQueue<Product2> outputQueue,
                               int consumerNodesNumber) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.consumerNodesNumber = consumerNodesNumber;
    }

    @Override
    public void run() {
        try {
            while(true) {
                Product2 product2 = inputQueue.take();
                if (product2 == END_QUEUE) {
                    for ( int i =0; i< consumerNodesNumber ; i++ )
                        outputQueue.put(END_QUEUE);
                } else {
                    outputQueue.put(product2);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
