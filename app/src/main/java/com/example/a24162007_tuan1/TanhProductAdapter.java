package com.example.a24162007_tuan1;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TanhProductAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final List<Product> products;

    private final NumberFormat priceFormat =
            NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public TanhProductAdapter(Context context, List<Product> products) {
        this.inflater = LayoutInflater.from(context);
        this.products = products;
    }

    @Override
    public int getCount() {
        return products.size();
    }

    @Override
    public Product getItem(int position) {
        return products.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(
            int position,
            View convertView,
            ViewGroup parent
    ) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(
                    R.layout.item_product_grid,
                    parent,
                    false
            );

            holder = new ViewHolder();
            holder.image = convertView.findViewById(R.id.imgProduct);
            holder.name = convertView.findViewById(R.id.tvProductName);
            holder.price = convertView.findViewById(R.id.tvProductPrice);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Product product = getItem(position);

        holder.image.setImageResource(product.getImage());
        holder.image.setContentDescription(product.getName());
        holder.name.setText(product.getName());
        holder.price.setText(priceFormat.format(product.getPrice()));

        return convertView;
    }

    private static class ViewHolder {
        ImageView image;
        TextView name;
        TextView price;
    }
}