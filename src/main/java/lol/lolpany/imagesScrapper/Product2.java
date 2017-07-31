package lol.lolpany.imagesScrapper;

public class Product2 {
    final static Product2 END_QUEUE = new Product2(999999999, "lol", null, null);

    final long id;
    final String sku;
    final String upc;
    final String name;

    public Product2(long id, String sku, String upc, String name) {
        this.id = id;
        this.sku = sku;
//        this.imagePath = imagePath;
        this.upc = upc;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product2 product2 = (Product2) o;

        if (id != product2.id) return false;
        return sku != null ? sku.equals(product2.sku) : product2.sku == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (sku != null ? sku.hashCode() : 0);
        return result;
    }
}
