package dev.gbppimentel.snapcompanion;
import android.app.*;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Game typography and ribbon controls extend into dialogs and dropdown lists. */
final class SnapDialog extends AlertDialog.Builder {
    private final GameAssets art;
    private boolean dark(){return getContext().getSharedPreferences("snap",0).getBoolean("darkMode",false);}
    SnapDialog(Context context,GameAssets art){super(context);this.art=art;}
    @Override public AlertDialog.Builder setTitle(CharSequence title){
        TextView view=new TextView(getContext());view.setText(title);view.setTypeface(art.heading);view.setTextSize(19);view.setTextColor(dark()?0xFFF2F5EC:0xFF543727);view.setPadding(art.dp(22),art.dp(20),art.dp(22),art.dp(10));return setCustomTitle(view);
    }
    @Override public AlertDialog create(){AlertDialog dialog=super.create();Window window=dialog.getWindow();if(window!=null)window.getDecorView().addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View view){view.post(()->style(dialog));}public void onViewDetachedFromWindow(View view){}});return dialog;}
    private void style(AlertDialog dialog){
        Window window=dialog.getWindow();if(window!=null){GradientDrawable paper=new GradientDrawable();paper.setColor(dark()?0xFF213C45:0xFFFFFAEB);paper.setCornerRadius(art.dp(12));paper.setStroke(art.dp(2),0xFFFFFFFF);window.setBackgroundDrawable(paper);art.fonts(window.getDecorView());}
        for(int id:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}){Button b=dialog.getButton(id);if(b!=null){b.setAllCaps(false);b.setTextColor(0xFF625240);b.setTypeface(art.body);b.setBackground(art.button());b.setPadding(art.dp(22),art.dp(8),art.dp(22),art.dp(8));}}
        ListView list=dialog.getListView();if(list!=null){list.setSelector(art.ribbon(true));list.setDividerHeight(art.dp(3));}
    }
    @Override public AlertDialog show(){AlertDialog dialog=create();dialog.show();return dialog;}
}
