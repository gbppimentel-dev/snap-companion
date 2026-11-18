package dev.gbppimentel.snapcompanion.next;
import android.app.AlertDialog;
import android.content.Context;
import android.text.*;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.Locale;

/** Read-only release coverage, including every original task in exactly three delivery batches. */
final class FeatureChecklist {
    static void show(Context context,String json)throws JSONException{
        JSONObject data=new JSONObject(json);JSONArray rows=data.getJSONArray("tasks");GameAssets art=new GameAssets(context);
        LinearLayout box=new LinearLayout(context);box.setOrientation(LinearLayout.VERTICAL);int pad=(int)(16*context.getResources().getDisplayMetrics().density);box.setPadding(pad,pad,pad,pad);
        TextView summary=new TextView(context);summary.setText("166 tasks · 3 batches. Live inputs start automatically. Historical manual records are archived; full project acceptance remains pending.");box.addView(summary);
        EditText query=new EditText(context);query.setHint("Search feature or task ID");box.addView(query);
        Spinner filter=new Spinner(context);filter.setBackgroundColor(android.graphics.Color.TRANSPARENT);filter.setAdapter(new ArrayAdapter<String>(context,android.R.layout.simple_spinner_item,new String[]{"All tasks","Implemented","Remaining"}){private View item(int p){TextView v=new TextView(context);v.setText(getItem(p)+"  ▾");v.setTypeface(art.body);v.setTextColor(0xFF543727);v.setPadding(pad,pad/2,pad,pad/2);v.setMinHeight(art.dp(48));v.setBackground(art.ribbon(false));return v;}public View getView(int p,View old,android.view.ViewGroup parent){return item(p);}public View getDropDownView(int p,View old,android.view.ViewGroup parent){return item(p);}});box.addView(filter);
        LinearLayout list=new LinearLayout(context);list.setOrientation(LinearLayout.VERTICAL);ScrollView scroll=new ScrollView(context);scroll.addView(list);box.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        Runnable fill=()->{list.removeAllViews();String q=query.getText().toString().toLowerCase(Locale.ROOT);int count=0;for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row==null)continue;boolean done=row.optString("status").equals("implemented");if(filter.getSelectedItemPosition()==1&&!done||filter.getSelectedItemPosition()==2&&done)continue;String content=row.optString("id")+" "+row.optString("description")+" "+row.optString("note");if(!content.toLowerCase(Locale.ROOT).contains(q))continue;count++;if(count>35)continue;TextView title=new TextView(context);title.setTextSize(16);title.setPadding(0,pad,0,0);title.setText((done?"☑ ":"☐ ")+row.optString("id")+" · Batch "+row.optInt("batch")+"\n"+row.optString("description"));list.addView(title);TextView note=new TextView(context);note.setText(row.optString("status").replace('-',' ')+" · "+row.optString("note"));note.setTextSize(14);list.addView(note);}TextView total=new TextView(context);total.setText(count+" matching tasks"+(count>35?" · first 35 shown; narrow the search":""));total.setPadding(0,pad,0,pad);list.addView(total);};
        query.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void afterTextChanged(Editable value){fill.run();}public void onTextChanged(CharSequence s,int a,int b,int c){}});
        filter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int position,long id){fill.run();}public void onNothingSelected(AdapterView<?> p){}});fill.run();
        AlertDialog dialog=new SnapDialog(context,art).setTitle("Full feature checklist").setView(box).setPositiveButton("Close",null).create();dialog.show();android.view.Window window=dialog.getWindow();if(window!=null)window.setLayout(-1,(int)(context.getResources().getDisplayMetrics().heightPixels*.9));
    }
}
