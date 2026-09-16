package com.nutrifit.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private LinearLayout content;
    private AppDatabase db;
    private ProfileStore store;
    private Profile profile;
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private LocalDate day=LocalDate.now();
    private int screenToken=0;
    private String currentScreen="home";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state); setContentView(R.layout.activity_main);
        content=findViewById(R.id.content);
        View scroll=findViewById(R.id.scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),
                insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); return insets;
        });
        scroll.requestApplyInsets();
        db=new AppDatabase(this); store=new ProfileStore(this); profile=store.load();
        if(state!=null) { day=LocalDate.parse(state.getString("day",day.toString())); currentScreen=state.getString("screen","home"); }
        if(profile==null || currentScreen.equals("profile")) showProfile();
        else if(currentScreen.equals("weight")) showWeight(); else showHome();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("day",day.toString()); out.putString("screen",currentScreen); super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() { super.onDestroy(); io.execute(db::close); io.shutdown(); }
    private void base(String screen,String title) {
        currentScreen=screen; screenToken++; content.removeAllViews(); text("NutriFit",30); text(title,21);
        if(profile!=null) {
            LinearLayout nav=new LinearLayout(this); content.addView(nav);
            navButton(nav,"Дневник",this::showHome); navButton(nav,"Вес",this::showWeight); navButton(nav,"Профиль",this::showProfile);
        }
    }
    private void navButton(LinearLayout row,String title,Runnable action) {
        Button b=new Button(this); b.setText(title); b.setTextSize(12); row.addView(b,new LinearLayout.LayoutParams(0,-2,1)); b.setOnClickListener(v->action.run());
    }
    private TextView text(String value,int size) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(Color.rgb(25,54,45)); t.setPadding(0,12,0,12); content.addView(t); return t;
    }
    private Button button(String label,Runnable action) {
        Button b=new Button(this); b.setText(label); content.addView(b); b.setOnClickListener(v->action.run()); return b;
    }
    private EditText input(String label,String value,boolean number) {
        text(label,14); EditText e=new EditText(this); e.setSingleLine(true); e.setText(value);
        e.setInputType(number ? InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL : InputType.TYPE_CLASS_TEXT);
        content.addView(e); return e;
    }
    private Spinner spinner(String label,String[] values,int selected) {
        text(label,14); Spinner s=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,values);
        s.setAdapter(a); s.setSelection(selected); content.addView(s); return s;
    }
    private void choices(String label,String[] ids,String[] names,Set<String> target) {
        button(label,()->{
            boolean[] checked=new boolean[ids.length]; for(int i=0;i<ids.length;i++) checked[i]=target.contains(ids[i]);
            new AlertDialog.Builder(this).setTitle(label).setMultiChoiceItems(names,checked,(d,i,on)->checked[i]=on)
                .setPositiveButton("Сохранить",(d,w)->{ target.clear(); for(int i=0;i<ids.length;i++) if(checked[i]) target.add(ids[i]); })
                .setNegativeButton("Отмена",null).show();
        });
    }
    private double number(EditText e) { return Double.parseDouble(e.getText().toString().trim().replace(',','.')); }
    private void error(Exception e) { new AlertDialog.Builder(this).setTitle("Проверьте данные").setMessage(e instanceof NumberFormatException ? "Заполните числовые поля корректно." : e.getMessage()).setPositiveButton("ОК",null).show(); }
    private String fmt(double x) { return String.format(Locale.getDefault(),"%.0f",x); }

    private void showProfile() {
        base("profile","Ваш профиль"); Profile draft=profile==null ? new Profile() : store.load();
        text("Учебная оценка питания для взрослых 18+. Каталог содержит примерные значения и состав; проверяйте этикетки и следы аллергенов.",14);
        EditText name=input("Имя",draft.name,false);
        EditText age=input("Возраст",Integer.toString(draft.age),true);
        EditText height=input("Рост, см",Double.toString(draft.height),true);
        EditText weight=input("Текущий вес, кг",Double.toString(draft.weight),true);
        EditText target=input("Желаемый вес, кг",Double.toString(draft.targetWeight),true);
        Spinner sex=spinner("Параметр пола для формулы",new String[]{"Мужской","Женский"},draft.male?0:1);
        Spinner activity=spinner("Активность",new String[]{"Низкая","Лёгкая","Средняя","Высокая"},draft.activity);
        Spinner goal=spinner("Цель",new String[]{"Снизить вес","Поддерживать вес","Набрать вес"},draft.goal);
        choices("Любимые продукты",Catalog.INGREDIENTS,Catalog.INGREDIENT_NAMES,draft.likes);
        choices("Нелюбимые продукты",Catalog.INGREDIENTS,Catalog.INGREDIENT_NAMES,draft.dislikes);
        choices("Аллергии",Catalog.ALLERGENS,Catalog.ALLERGEN_NAMES,draft.allergies);
        choices("Пищевые ограничения",Catalog.RESTRICTIONS,Catalog.RESTRICTION_NAMES,draft.restrictions);
        button("Сохранить профиль",()->{
            try {
                draft.name=name.getText().toString().trim(); if(draft.name.isEmpty()) throw new IllegalArgumentException("Введите имя.");
                double a=number(age); if(a!=Math.floor(a)) throw new IllegalArgumentException("Возраст — целое число.");
                draft.age=(int)a; draft.height=number(height); draft.weight=number(weight); draft.targetWeight=number(target);
                draft.male=sex.getSelectedItemPosition()==0; draft.activity=activity.getSelectedItemPosition(); draft.goal=goal.getSelectedItemPosition();
                NutritionCalculator.calculate(draft);
                if(!Collections.disjoint(draft.likes,draft.dislikes)) throw new IllegalArgumentException("Продукт не может быть одновременно любимым и нелюбимым.");
                write(()->{ db.saveWeight(LocalDate.now().toString(),draft.weight); store.save(draft); },()->{ profile=draft; showHome(); });
            } catch(Exception e) { error(e); }
        });
    }
    private void write(Runnable work,Runnable after) {
        int token=screenToken;
        io.execute(()->{ try { work.run(); runOnUiThread(()->{ if(!isDestroyed()) {
            profile=store.load();
            if(token==screenToken) after.run();
            else if(currentScreen.equals("home")) showHome();
            else if(currentScreen.equals("weight")) showWeight();
        } }); }
            catch(Exception e) { runOnUiThread(()->{ if(!isDestroyed()) error(e); }); } });
    }
    private void showHome() {
        base("home",profile.name+" · дневник");
        button("Дата: "+day,()->{
            DatePickerDialog d=new DatePickerDialog(this,(v,y,m,n)->{day=LocalDate.of(y,m+1,n);showHome();},day.getYear(),day.getMonthValue()-1,day.getDayOfMonth());
            d.getDatePicker().setMaxDate(System.currentTimeMillis()); d.show();
        });
        int token=screenToken; String selectedDay=day.toString(); text("Загрузка…",16);
        io.execute(()->{
            try {
                List<AppDatabase.Entry> entries=db.diary(selectedDay);
                runOnUiThread(()->{
                    if(isDestroyed() || token!=screenToken) return;
                    content.removeViewAt(content.getChildCount()-1); renderDiary(entries);
                });
            } catch(Exception e) { runOnUiThread(()->{ if(!isDestroyed()) error(e); }); }
        });
    }
    private void renderDiary(List<AppDatabase.Entry> entries) {
        NutritionCalculator.Target t=NutritionCalculator.calculate(profile);
        double kcal=0,p=0,f=0,c=0;
        for(AppDatabase.Entry e:entries) {kcal+=e.kcal;p+=e.protein;f+=e.fat;c+=e.carbs;}
        double remaining=t.calories-kcal;
        text(fmt(kcal)+" / "+fmt(t.calories)+" ккал",28);
        text(remaining>=0 ? "Осталось: "+fmt(remaining)+" ккал" : "Выше ориентира на "+fmt(-remaining)+" ккал",18);
        text("Б: "+fmt(p)+" / "+fmt(t.protein)+" г   Ж: "+fmt(f)+" / "+fmt(t.fat)+" г   У: "+fmt(c)+" / "+fmt(t.carbs)+" г",15);
        if(!day.equals(LocalDate.now())) text("Для прошлых дат показан текущий ориентир профиля.",13);
        button("+ Добавить еду",()->foodDialog(null,200));
        text("Приёмы пищи",21);
        if(entries.isEmpty()) text("Пока нет записей. Добавьте первый приём пищи.",16);
        for(AppDatabase.Entry e:entries) {
            text(e.meal+" · "+e.name+"\n"+fmt(e.grams)+" г · "+fmt(e.kcal)+" ккал",16);
            button("Удалить запись",()->new AlertDialog.Builder(this).setMessage("Удалить «"+e.name+"»?")
                .setPositiveButton("Удалить",(d,w)->write(()->db.delete(e.id),this::showHome)).setNegativeButton("Отмена",null).show());
        }
        if(day.equals(LocalDate.now())) {
            text("Что можно съесть?",21);
            List<RecommendationEngine.Suggestion> suggestions=RecommendationEngine.recommend(Catalog.FOODS,profile,remaining);
            if(suggestions.isEmpty()) text(remaining<=0 ? "Дневной ориентир достигнут. Рекомендаций по остатку нет." : "В каталоге нет подходящей порции. Фильтры не ослабляются.",16);
            for(RecommendationEngine.Suggestion s:suggestions)
                button(s.food.name+" · "+s.grams+" г · "+fmt(s.calories)+" ккал",()->foodDialog(s.food,s.grams));
            text("Подбор учитывает выбранные ограничения и состав учебного каталога. Он не подтверждает отсутствие следов аллергенов.",13);
        }
    }
    private void foodDialog(Food selected,int portion) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(24,12,24,12);
        Spinner foods=new Spinner(this); foods.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Catalog.FOODS));
        if(selected!=null) foods.setSelection(Catalog.FOODS.indexOf(selected)); box.addView(foods);
        Spinner meal=new Spinner(this); String[] meals={"Завтрак","Обед","Ужин","Перекус"};
        meal.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,meals)); box.addView(meal);
        TextView label=new TextView(this); label.setText("Вес готового блюда, г"); box.addView(label);
        EditText grams=new EditText(this); grams.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL); grams.setText(Integer.toString(portion)); box.addView(grams);
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Добавить еду").setView(box).setPositiveButton("Добавить",null).setNegativeButton("Отмена",null).create();
        String selectedDay=day.toString(); dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            try {
                double amount=number(grams); if(!Double.isFinite(amount)||amount<1||amount>2000) throw new IllegalArgumentException("Порция: 1–2000 г.");
                Food food=(Food)foods.getSelectedItem();
                if(!Collections.disjoint(food.allergens,profile.allergies)) throw new IllegalArgumentException("В составе указан выбранный вами аллерген.");
                String mealName=meals[meal.getSelectedItemPosition()];
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                write(()->db.add(selectedDay,mealName,food,amount),this::showHome); dialog.dismiss();
            } catch(Exception e) { error(e); }
        });
    }
    private void showWeight() {
        base("weight","История веса");
        text("Цель: "+profile.targetWeight+" кг. Повторная запись заменяет вес за ту же дату.",16);
        EditText value=input("Вес сегодня, кг",Double.toString(profile.weight),true);
        button("Записать вес",()->{
            try {
                double kg=number(value); if(!Double.isFinite(kg)||kg<35||kg>250) throw new IllegalArgumentException("Вес: 35–250 кг.");
                Profile updated=store.load(); updated.weight=kg;
                boolean reached=updated.goal==0 && kg<=updated.targetWeight || updated.goal==2 && kg>=updated.targetWeight;
                if(reached) updated.goal=1;
                write(()->{ db.saveWeight(LocalDate.now().toString(),kg); store.save(updated); },()->{
                    profile=updated;
                    if(reached) Toast.makeText(this,"Цель достигнута. Включено поддержание веса.",Toast.LENGTH_LONG).show();
                    showWeight();
                });
            } catch(Exception e) { error(e); }
        });
        int token=screenToken;
        io.execute(()->{
            try {
                List<AppDatabase.Weight> weights=db.weights(); runOnUiThread(()->{
                    if(isDestroyed() || token!=screenToken) return;
                    if(weights.isEmpty()) {text("Пока нет измерений.",16);return;}
                    WeightChartView chart=new WeightChartView(this,weights);
                    content.addView(chart,new LinearLayout.LayoutParams(-1,(int)(250*getResources().getDisplayMetrics().density)));
                    text("Изменение: "+String.format(Locale.getDefault(),"%+.1f",weights.get(weights.size()-1).kg-weights.get(0).kg)+" кг",20);
                    for(int i=weights.size()-1;i>=0;i--) text(weights.get(i).day+" — "+weights.get(i).kg+" кг",16);
                });
            } catch(Exception e) { runOnUiThread(()->{if(!isDestroyed()) error(e);}); }
        });
    }
}
