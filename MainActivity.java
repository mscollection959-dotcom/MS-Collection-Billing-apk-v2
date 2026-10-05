
package com.mscollection.billing;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    LinearLayout root, itemsBox;
    EditText customer, phone, itemName, qty, rate, discount;
    TextView totalText, printerStatus, billNoText;
    BluetoothAdapter btAdapter; BluetoothSocket socket; OutputStream out;
    SharedPreferences prefs;
    long billNo;
    ArrayList<Item> items = new ArrayList<>();
    ArrayList<String> history = new ArrayList<>();

    static class Item {
        String name; double qty, rate;
        Item(String n,double q,double r){name=n;qty=q;rate=r;}
        double total(){return qty*rate;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        prefs=getSharedPreferences("billing",MODE_PRIVATE);
        billNo=prefs.getLong("billNo",1);
        btAdapter=BluetoothAdapter.getDefaultAdapter();
        history.addAll(prefs.getStringSet("history",new LinkedHashSet<>()));
        showHome();
        requestBtPermission();
    }

    TextView tv(String s,float z,boolean bold){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(z); t.setPadding(0,8,0,8);
        if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD); return t;
    }
    EditText edit(String hint){
        EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true);
        e.setTextSize(16); e.setPadding(12,8,12,8); return e;
    }
    Button btn(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(15);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2); p.setMargins(0,6,0,6);
        b.setLayoutParams(p); return b;
    }
    void base(String title){
        ScrollView sc=new ScrollView(this); root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(22,18,22,28); sc.addView(root); setContentView(sc);
        TextView h=tv(title,25,true); h.setGravity(Gravity.CENTER); root.addView(h);
    }

    void showHome(){
        base("MS COLLECTION");
        TextView sub=tv("Fashion for Everyone",15,false); sub.setGravity(Gravity.CENTER); root.addView(sub);
        Button b=btn("🧾  NEW BILL"); b.setOnClickListener(v->showBilling()); root.addView(b);
        b=btn("📋  BILL HISTORY"); b.setOnClickListener(v->showHistory()); root.addView(b);
        b=btn("⚙  SHOP / UPI SETTINGS"); b.setOnClickListener(v->showSettings()); root.addView(b);
        b=btn("🖨  PRINTER"); b.setOnClickListener(v->printerMenu()); root.addView(b);
        TextView info=tv("\n58mm MPT-II • Bluetooth • ESC/POS\nNo Ads • No Subscription • Offline",14,false);
        info.setGravity(Gravity.CENTER); root.addView(info);
    }

    void showBilling(){
        base("NEW BILL");
        billNoText=tv("Bill No: MC-"+String.format(Locale.US,"%05d",billNo),16,true); root.addView(billNoText);
        customer=edit("Customer Name"); phone=edit("Mobile Number");
        phone.setInputType(InputType.TYPE_CLASS_PHONE); root.addView(customer); root.addView(phone);
        root.addView(tv("ITEMS",18,true));
        itemName=edit("Item Name"); qty=edit("Qty"); rate=edit("Rate");
        qty.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rate.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        root.addView(itemName);root.addView(qty);root.addView(rate);
        Button a=btn("＋ Add Item");a.setOnClickListener(v->addItem());root.addView(a);
        itemsBox=new LinearLayout(this);itemsBox.setOrientation(LinearLayout.VERTICAL);root.addView(itemsBox);
        discount=edit("Discount (₹)"); discount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        discount.setText("0"); discount.setOnFocusChangeListener((v,f)->refreshItems()); root.addView(discount);
        totalText=tv("TOTAL: ₹0.00",22,true);totalText.setGravity(Gravity.RIGHT);root.addView(totalText);
        Button p=btn("🖨 PRINT BILL");p.setOnClickListener(v->printBill());root.addView(p);
        p=btn("📲 WHATSAPP / SHARE BILL");p.setOnClickListener(v->shareBill());root.addView(p);
        p=btn("← BACK");p.setOnClickListener(v->showHome());root.addView(p);
    }

    void addItem(){
        String n=itemName.getText().toString().trim(); if(n.isEmpty()){toast("Item name डालें");return;}
        double q=num(qty.getText().toString(),1), r=num(rate.getText().toString(),0);
        if(r<=0){toast("Rate डालें");return;} items.add(new Item(n,q,r));refreshItems();
        itemName.setText("");qty.setText("");rate.setText("");
    }
    double num(String s,double d){try{return Double.parseDouble(s);}catch(Exception e){return d;}}
    String money(double d){return String.format(Locale.US,"%.2f",d);}
    double subtotal(){double x=0;for(Item i:items)x+=i.total();return x;}
    double discount(){return Math.min(num(discount==null?"0":discount.getText().toString(),0),subtotal());}
    double grand(){return Math.max(0,subtotal()-discount());}

    void refreshItems(){
        if(itemsBox==null)return; itemsBox.removeAllViews(); int i=1;
        for(Item x:items){
            LinearLayout row=new LinearLayout(this);
            TextView t=tv(i+". "+x.name+" • "+x.qty+" × ₹"+money(x.rate)+" = ₹"+money(x.total()),15,false);
            row.addView(t,new LinearLayout.LayoutParams(0,-2,1)); Button d=new Button(this);d.setText("X");
            final int idx=i-1;d.setOnClickListener(v->{items.remove(idx);refreshItems();});row.addView(d);itemsBox.addView(row);i++;
        }
        totalText.setText("TOTAL: ₹"+money(grand()));
    }

    void showSettings(){
        base("SHOP / UPI SETTINGS");
        EditText name=edit("Shop Name"); name.setText(prefs.getString("shop","MS COLLECTION"));
        EditText addr=edit("Shop Address"); addr.setText(prefs.getString("address",""));
        EditText gst=edit("GSTIN"); gst.setText(prefs.getString("gstin",""));
        EditText upi=edit("UPI ID (example: shop@upi)"); upi.setText(prefs.getString("upi",""));
        EditText upiname=edit("UPI Payee Name"); upiname.setText(prefs.getString("upiname","MS COLLECTION"));
        root.addView(name);root.addView(addr);root.addView(gst);root.addView(upi);root.addView(upiname);
        Button save=btn("💾 SAVE SETTINGS");save.setOnClickListener(v->{
            prefs.edit().putString("shop",name.getText().toString()).putString("address",addr.getText().toString())
              .putString("gstin",gst.getText().toString()).putString("upi",upi.getText().toString())
              .putString("upiname",upiname.getText().toString()).apply();toast("Settings saved");showHome();
        });root.addView(save);
        Button back=btn("← BACK");back.setOnClickListener(v->showHome());root.addView(back);
    }

    void showHistory(){
        base("BILL HISTORY");
        if(history.isEmpty()) root.addView(tv("No bills saved yet.",16,false));
        else for(String s:history){
            TextView t=tv(s,14,false);t.setPadding(0,14,0,14);root.addView(t);
        }
        Button b=btn("← BACK");b.setOnClickListener(v->showHome());root.addView(b);
    }

    void printerMenu(){
        base("MPT-II PRINTER");
        printerStatus=tv("Printer: "+(socket!=null&&socket.isConnected()?"Connected":"Not Connected"),16,true);root.addView(printerStatus);
        Button b=btn("🔵 CONNECT PAIRED MPT-II");b.setOnClickListener(v->choosePrinter());root.addView(b);
        b=btn("🧪 TEST PRINT");b.setOnClickListener(v->testPrint());root.addView(b);
        b=btn("← BACK");b.setOnClickListener(v->showHome());root.addView(b);
    }

    void requestBtPermission(){
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN},100);
    }
    void choosePrinter(){
        if(btAdapter==null){toast("Bluetooth उपलब्ध नहीं है");return;}
        if(Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED){requestBtPermission();return;}
        if(!btAdapter.isEnabled()){startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),200);return;}
        ArrayList<BluetoothDevice> ds=new ArrayList<>(btAdapter.getBondedDevices());
        if(ds.isEmpty()){toast("Bluetooth Settings में MPT-II pair करें");return;}
        String[] names=new String[ds.size()];for(int i=0;i<ds.size();i++)names[i]=ds.get(i).getName()+"\n"+ds.get(i).getAddress();
        new AlertDialog.Builder(this).setTitle("Printer चुनें").setItems(names,(d,w)->connect(ds.get(w))).show();
    }
    void connect(BluetoothDevice d){
        new Thread(()->{try{
            if(socket!=null)try{socket.close();}catch(Exception e){}
            socket=d.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));socket.connect();out=socket.getOutputStream();
            runOnUiThread(()->{if(printerStatus!=null)printerStatus.setText("Printer: Connected • "+d.getName());toast("Printer connected");});
        }catch(Exception e){runOnUiThread(()->toast("Connection failed"));}}).start();
    }
    boolean connected(){if(socket!=null&&socket.isConnected()&&out!=null)return true;toast("पहले printer connect करें");return false;}
    void esc(char c){try{out.write(new byte[]{0x1B,(byte)c});}catch(Exception e){}}
    void esc(char c,int n){try{out.write(new byte[]{0x1B,(byte)c,(byte)n});}catch(Exception e){}}
    void put(String s)throws Exception{out.write(s.getBytes(StandardCharsets.UTF_8));}

    String billText(){
        String shop=prefs.getString("shop","MS COLLECTION"), addr=prefs.getString("address",""), gst=prefs.getString("gstin","");
        StringBuilder s=new StringBuilder();
        s.append(shop).append("\n");if(!addr.isEmpty())s.append(addr).append("\n");if(!gst.isEmpty())s.append("GSTIN: ").append(gst).append("\n");
        s.append("--------------------------------\n");
        s.append("Bill No: MC-").append(String.format(Locale.US,"%05d",billNo)).append("\n");
        s.append("Date: ").append(new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).format(new Date())).append("\n");
        if(customer!=null&&!customer.getText().toString().trim().isEmpty())s.append("Customer: ").append(customer.getText()).append("\n");
        if(phone!=null&&!phone.getText().toString().trim().isEmpty())s.append("Mobile: ").append(phone.getText()).append("\n");
        s.append("--------------------------------\n");
        for(Item x:items)s.append(x.name).append("  ").append(x.qty).append(" x ").append(money(x.rate)).append(" = ").append(money(x.total())).append("\n");
        s.append("--------------------------------\nSubtotal: ₹").append(money(subtotal())).append("\nDiscount: ₹").append(money(discount())).append("\nTOTAL: ₹").append(money(grand())).append("\n");
        s.append("--------------------------------\nTHANK YOU!\nVISIT AGAIN ❤️\n");
        return s.toString();
    }

    void testPrint(){
        if(!connected())return;new Thread(()->{try{esc('@');esc('a',1);esc('E',1);put("MS COLLECTION\n");esc('E',0);put("MPT-II 58mm TEST PRINT\nBluetooth / ESC-POS OK\n\n\n");runOnUiThread(()->toast("Test print भेज दिया"));}catch(Exception e){}}).start();
    }

    void printBill(){
        if(items.isEmpty()){toast("Item जोड़ें");return;}if(!connected())return;
        final String text=billText();final String summary="MC-"+String.format(Locale.US,"%05d",billNo)+" • ₹"+money(grand())+" • "+new SimpleDateFormat("dd-MM-yyyy HH:mm",Locale.US).format(new Date());
        new Thread(()->{try{
            esc('@');esc('a',1);esc('E',1);put(prefs.getString("shop","MS COLLECTION")+"\n");esc('E',0);esc('a',0);
            put(text);put("\n\n\n");
            runOnUiThread(()->{history.add(0,summary);if(history.size()>100)history.remove(history.size()-1);prefs.edit().putLong("billNo",billNo+1).putStringSet("history",new LinkedHashSet<>(history)).apply();billNo++;toast("Bill printed & saved");});
        }catch(Exception e){runOnUiThread(()->toast("Print error"));}}).start();
    }

    void shareBill(){
        if(items.isEmpty()){toast("Item जोड़ें");return;}
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,billText());
        startActivity(Intent.createChooser(i,"Bill WhatsApp / Share"));
    }
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override protected void onDestroy(){super.onDestroy();try{if(socket!=null)socket.close();}catch(Exception e){}}
}
