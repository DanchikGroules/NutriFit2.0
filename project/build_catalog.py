"""Editable, original demonstration catalog. Values are approximate per 100 g.
Recipe instructions are authored for this project, not copied from linked videos.
Run after changing the data; tests validate IDs, quantities and recipe counts.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
ASSETS.mkdir(parents=True, exist_ok=True)
# id | display name | kcal | protein | fat | carbs | allergens | classification
DATA = '''oats|Овсяные хлопья, сухие|370|13|7|62|gluten|plant
rice|Рис белый, сухой|350|7|1|78||plant
buckwheat|Гречка, сухая|343|13|3|64||plant
bulgur|Булгур, сухой|342|12|1|76|gluten|plant
couscous|Кускус, сухой|360|12|2|72|gluten|plant
quinoa|Киноа, сухая|368|14|6|64||plant
millet|Пшено, сухое|348|11|3|69||plant
pasta|Макароны, сухие|350|12|2|70|gluten|plant
bread|Хлеб цельнозерновой|247|9|4|41|gluten|plant
lavash|Лаваш пшеничный|277|9|1|56|gluten|plant
flour|Мука пшеничная|334|10|1|70|gluten|plant
chicken|Куриное филе, сырое|120|23|2|0||meat
turkey|Филе индейки, сырое|114|24|1|0||meat
beef|Говядина нежирная, сырая|158|21|8|0||meat
chicken_cooked|Куриное филе, готовое|165|31|4|0||meat
turkey_cooked|Индейка, готовая|150|29|3|0||meat
salmon|Лосось, сырой|208|20|13|0|fish|fish
cod|Треска, сырая|82|18|1|0|fish|fish
tuna|Тунец в собственном соку|116|26|1|0|fish|fish
shrimp|Креветки очищенные, готовые|99|24|0|0|shellfish|fish
egg|Яйцо без скорлупы|143|13|10|1|egg|egg
egg_cooked|Яйцо варёное|155|13|11|1|egg|egg
milk|Молоко 2,5%|52|3|3|5|milk|dairy
yogurt|Йогурт натуральный|60|4|3|5|milk|dairy
greek_yogurt|Йогурт греческий 2%|73|10|2|4|milk|dairy
cottage|Творог 5%|121|17|5|2|milk|dairy
feta|Сыр фета|264|14|21|4|milk|dairy
cheese|Сыр полутвёрдый|350|25|27|0|milk|dairy
mozzarella|Моцарелла|250|18|19|2|milk|dairy
kefir|Кефир 2,5%|53|3|3|4|milk|dairy
tofu|Тофу|76|8|5|2|soy|plant
chickpeas|Нут отварной|164|9|3|27||plant
beans|Фасоль отварная|127|9|1|23||plant
lentils|Чечевица отварная|116|9|0|20||plant
peas|Горошек зелёный|81|5|0|14||plant
corn|Кукуруза консервированная|96|3|2|19||plant
potato|Картофель, сырой|77|2|0|17||plant
sweet_potato|Батат, сырой|86|2|0|20||plant
tomato|Помидор|18|1|0|4||plant
cucumber|Огурец|15|1|0|3||plant
pepper|Перец сладкий|31|1|0|6||plant
carrot|Морковь|41|1|0|10||plant
onion|Лук репчатый|40|1|0|9||plant
zucchini|Кабачок|17|1|0|3||plant
eggplant|Баклажан|25|1|0|6||plant
broccoli|Брокколи|34|3|0|7||plant
cauliflower|Цветная капуста|25|2|0|5||plant
cabbage|Капуста белокочанная|25|1|0|6||plant
spinach|Шпинат|23|3|0|4||plant
lettuce|Листовой салат|15|1|0|3||plant
mushroom|Шампиньоны|22|3|0|3||plant
pumpkin|Тыква|26|1|0|7||plant
beet_cooked|Свёкла отварная|44|2|0|10||plant
avocado|Авокадо|160|2|15|9||plant
apple|Яблоко|52|0|0|14||plant
banana|Банан|89|1|0|23||plant
pear|Груша|57|0|0|15||plant
orange|Апельсин, мякоть|47|1|0|12||plant
kiwi|Киви|61|1|1|15||plant
berries|Ягоды, смесь|45|1|0|10||plant
strawberry|Клубника|32|1|0|8||plant
blueberry|Черника|57|1|0|14||plant
mango|Манго|60|1|0|15||plant
peach|Персик|39|1|0|10||plant
pineapple|Ананас|50|1|0|13||plant
lemon|Лимон|29|1|0|9||plant
walnut|Грецкий орех|654|15|65|14|nuts|plant
almond|Миндаль|579|21|50|22|nuts|plant
peanut|Арахис|567|26|49|16|peanut|plant
sesame|Кунжут|573|18|50|23|sesame|plant
chia|Семена чиа|486|17|31|42||plant
flax|Семена льна|534|18|42|29||plant
pumpkin_seed|Семечки тыквенные|559|30|49|11||plant
raisins|Изюм|299|3|0|79||plant
apricot_dried|Курага|241|3|1|63||plant
dates|Финики|282|2|0|75||plant
olive_oil|Масло оливковое|884|0|100|0||plant
butter|Масло сливочное|748|1|83|1|milk|dairy
honey|Мёд|304|0|0|82||honey
cocoa|Какао без сахара|228|20|14|58||plant
tomato_passata|Томатная пассата|29|1|0|5||plant
coconut_milk|Молоко кокосовое|197|2|21|3||plant
soy_milk|Напиток соевый|33|3|2|1|soy|plant
soy_sauce|Соус соевый|53|8|0|5|soy,gluten|plant
garlic|Чеснок|149|6|1|33||plant
herbs|Зелень свежая|36|3|1|6||plant
water|Вода питьевая|0|0|0|0||plant
rice_cooked|Рис отварной|130|3|0|28||plant
buckwheat_cooked|Гречка отварная|110|4|1|21||plant
pasta_cooked|Макароны отварные|131|5|1|25|gluten|plant
oatmeal_cooked|Овсяная каша на воде|71|3|2|12|gluten|plant
potato_cooked|Картофель отварной|87|2|0|20||plant
rye_bread|Хлеб ржаной|210|6|1|43|gluten|plant
cream|Сливки 10%|118|3|10|4|milk|dairy
ricotta|Рикотта|174|11|13|3|milk|dairy
plum|Слива|46|1|0|11||plant
grapes|Виноград|69|1|0|18||plant
melon|Дыня|34|1|0|8||plant
radish|Редис|16|1|0|3||plant
green_beans|Фасоль стручковая|31|2|0|7||plant'''
products=[]
for line in DATA.splitlines():
    key,title,kcal,p,f,c,allergens,kind=line.split('|')
    products.append(dict(id=key,title=title,kcal=float(kcal),protein=float(p),fat=float(f),carbs=float(c),allergens=allergens,kind=kind))
P={p['id']:p for p in products}
recipes=[]
def add(title,category,minutes,grams,ingredients,steps):
    pairs=[(part.split(':')[0],float(part.split(':')[1])) for part in ingredients.split()]
    allergens=sorted({a for key,g in pairs for a in P[key]['allergens'].split(',') if a})
    kinds={P[key]['kind'] for key,g in pairs}
    tags=[]
    if not kinds.intersection({'meat','fish'}):tags.append('vegetarian')
    if kinds=={'plant'}:tags.append('vegan')
    if 'milk' not in allergens:tags.append('lactose_free')
    if 'gluten' not in allergens:tags.append('gluten_free')
    values={macro:round(sum(P[key][macro]*g/100 for key,g in pairs),1) for macro in ['kcal','protein','fat','carbs']}
    recipes.append(dict(id='recipe_%03d'%(len(recipes)+1),title=title,category=category,minutes=minutes,grams=grams,
        ingredients=[dict(id=key,name=P[key]['title'],grams=g) for key,g in pairs],steps=steps,
        allergens=','.join(allergens),tags=','.join(tags),**values))

# Ten distinct ingredient compositions per section. Amounts describe ONE serving.
porridges=[('Овсянка с бананом','oats:50 milk:150 water:100 banana:100'),('Овсянка с яблоком и орехами','oats:50 water:200 apple:100 walnut:15'),('Пшённая каша с тыквой','millet:50 milk:150 water:150 pumpkin:100'),('Рисовая каша с грушей','rice:50 milk:150 water:150 pear:100'),('Гречневая каша с грибами','buckwheat:60 water:160 mushroom:100 onion:30 olive_oil:5'),('Овсянка с какао и вишнёвым миксом','oats:50 milk:150 water:80 berries:80 cocoa:5'),('Киноа с манго','quinoa:50 water:150 mango:100 yogurt:80'),('Каша с изюмом и курагой','oats:50 water:200 raisins:15 apricot_dried:20'),('Булгур с яблоком','bulgur:50 water:150 apple:100 honey:10'),('Овсянка с ягодами и семенами','oats:50 soy_milk:180 water:80 berries:80 flax:10')]
for title,ing in porridges:
    add(title,'breakfast',25,350,ing,['Промойте крупу, если это требуется на упаковке. Сухофрукты ополосните, крупные кусочки нарежьте.','Залейте крупу указанной жидкостью. Варите на небольшом огне по времени на упаковке, помешивая; при необходимости добавьте немного воды.','Тыкву добавьте в начале варки; грибы и лук, если они есть в составе, отдельно потушите на указанном масле до готовности.','Смешайте готовую кашу с остальными ингредиентами. Свежие фрукты, ягоды, йогурт и семена добавьте перед подачей.'])
eggs=[('Омлет с томатами','tomato:120 herbs:5'),('Омлет со шпинатом','spinach:70 cheese:20'),('Омлет с шампиньонами','mushroom:100 onion:30'),('Омлет с брокколи','broccoli:120 cheese:20'),('Омлет с перцем','pepper:100 onion:30'),('Омлет с кабачком','zucchini:120 herbs:5'),('Омлет с курицей','chicken_cooked:80 tomato:80'),('Омлет с фетой','feta:40 spinach:50'),('Омлет с зелёным горошком','peas:100 herbs:5'),('Омлет с моцареллой','mozzarella:40 tomato:100')]
for title,extra in eggs:
    add(title,'eggs',20,280,'egg:100 milk:40 olive_oil:5 '+extra,['Нарежьте начинку небольшими кусочками. Брокколи предварительно отварите до мягкости; готовую курицу разберите на кусочки.','Разогрейте масло, добавьте овощи и грибы, если они указаны. Готовьте, пока овощи не станут мягкими, а лишняя жидкость не испарится.','Взбейте яйца с молоком, вылейте в сковороду. Добавьте готовую курицу, горошек или сыр согласно составу.','Накройте крышкой и готовьте на слабом огне до полного схватывания яиц. Посыпьте указанной зеленью.'])
salads=[('Греческий салат','tomato:120 cucumber:100 pepper:60 feta:40 olive_oil:5'),('Салат с тунцом и фасолью','tuna:100 beans:100 tomato:100 lettuce:30 olive_oil:5'),('Салат с курицей и авокадо','chicken_cooked:100 avocado:60 cucumber:100 lettuce:30 lemon:10'),('Свёкла с орехами','beet_cooked:180 walnut:20 yogurt:50'),('Нут с овощами','chickpeas:150 cucumber:100 tomato:100 olive_oil:5 lemon:10'),('Капустный салат с яблоком','cabbage:150 apple:100 carrot:60 yogurt:50'),('Салат с яйцом и редисом','egg_cooked:100 radish:100 cucumber:100 yogurt:40 herbs:5'),('Салат с креветками и манго','shrimp:120 mango:100 avocado:50 lettuce:30 lemon:10'),('Салат с чечевицей','lentils:150 tomato:100 pepper:80 olive_oil:5'),('Салат с моцареллой','mozzarella:80 tomato:180 herbs:10 olive_oil:5')]
for title,ing in salads:
    add(title,'salad',15,int(sum(float(x.split(':')[1]) for x in ing.split())),ing,['Вымойте свежие овощи, фрукты и зелень. Используйте уже отваренные бобовые, яйца, свёклу, курицу и очищенные готовые креветки, если они входят в состав.','Нарежьте крупные ингредиенты. С консервированного тунца и бобовых слейте жидкость.','Смешайте ингредиенты; заправьте указанными йогуртом, маслом или лимоном. Орехи и зелень добавьте в конце.'])
soups=[('Тыквенный крем-суп','pumpkin:200 carrot:50 onion:30 yogurt:40'),('Суп с чечевицей и томатами','lentils:160 tomato_passata:100 carrot:50 onion:30'),('Куриный суп с рисом','chicken:100 rice:30 carrot:50 onion:30'),('Овощной суп с фасолью','beans:130 potato:80 cabbage:70 carrot:40'),('Грибной суп','mushroom:160 potato:100 onion:30 cream:40'),('Крем-суп из брокколи','broccoli:200 potato:80 onion:30 yogurt:40'),('Суп с индейкой и булгуром','turkey:100 bulgur:30 carrot:50 onion:30'),('Суп с треской','cod:120 potato:100 carrot:50 onion:30'),('Минестроне с макаронами','pasta:30 beans:70 zucchini:60 tomato_passata:100'),('Суп с нутом и шпинатом','chickpeas:130 spinach:70 carrot:50 onion:30')]
for title,extra in soups:
    add(title,'soup',40,550,'water:300 olive_oil:5 '+extra,['Нарежьте овощи небольшими одинаковыми кусочками. Если в составе есть сырая птица или рыба, используйте для неё отдельную доску.','В кастрюле прогрейте масло и лук, если он указан. Добавьте воду, твёрдые овощи и сырую птицу. Доведите до кипения и уменьшите огонь.','Добавьте крупу или пасту с учётом времени на упаковке. Готовые бобовые, шпинат и рыбу добавляйте ближе к концу. Готовьте до мягкости овощей и полной готовности всех сырых ингредиентов.','Для крем-супа без мяса измельчите овощи погружным блендером, сняв кастрюлю с огня. Молочные ингредиенты добавьте в конце и прогрейте.'])
pastas=[('Паста с курицей и томатами','chicken_cooked:100 tomato_passata:120'),('Паста с тунцом','tuna:100 tomato:120'),('Паста со шпинатом и рикоттой','spinach:100 ricotta:70'),('Паста с грибами','mushroom:150 cream:60'),('Паста с брокколи и сыром','broccoli:150 cheese:30'),('Паста с чечевичным соусом','lentils:130 tomato_passata:120'),('Паста с креветками','shrimp:120 tomato:120'),('Паста с кабачком','zucchini:180 cheese:25'),('Паста с баклажаном','eggplant:150 tomato_passata:100'),('Паста с индейкой и перцем','turkey_cooked:100 pepper:120')]
for title,extra in pastas:
    add(title,'pasta',25,380,'pasta:70 olive_oil:5 '+extra,['Отварите макароны по инструкции на упаковке. Перед сливом сохраните немного воды от варки.','Нарежьте овощи и грибы, прогрейте их на масле и потушите до мягкости. Брокколи можно отварить вместе с пастой в последние минуты.','Добавьте остальные ингредиенты соуса: готовую птицу, готовые креветки, тунец или бобовые — согласно составу. Сыры и сливки добавляйте на слабом огне.','Смешайте пасту с соусом и небольшим количеством воды от варки. Прогрейте и подавайте.'])
bowls=[('Боул с курицей и гречкой','buckwheat:60 chicken_cooked:100 cucumber:100 tomato:80'),('Боул с индейкой и рисом','rice:60 turkey_cooked:100 pepper:80 cucumber:80'),('Боул с киноа и нутом','quinoa:60 chickpeas:100 tomato:100 spinach:30'),('Боул с булгуром и фетой','bulgur:60 feta:50 cucumber:100 tomato:100'),('Боул с кускусом и тунцом','couscous:60 tuna:100 tomato:100 lettuce:30'),('Боул с рисом и тофу','rice:60 tofu:140 carrot:70 cucumber:80'),('Боул с чечевицей и гречкой','buckwheat:50 lentils:120 beet_cooked:100 herbs:5'),('Боул с киноа и креветками','quinoa:60 shrimp:120 avocado:50 cucumber:80'),('Боул с пшеном и фасолью','millet:50 beans:120 tomato:100 pepper:80'),('Боул с рисом и яйцом','rice:60 egg_cooked:100 peas:70 carrot:60')]
for title,extra in bowls:
    add(title,'bowl',30,430,'olive_oil:5 lemon:10 '+extra,['Сварите сухую крупу по инструкции на упаковке. Кускус приготовьте способом, указанным производителем.','Вымойте и нарежьте овощи. Морковь можно натереть или слегка потушить; тофу при желании подрумяньте на части указанного масла.','Используйте готовую птицу, очищенные варёные креветки, варёные яйца и бобовые согласно списку. Разложите их рядом с крупой.','Добавьте овощи, зелень или сыр. Смешайте масло с лимоном и полейте готовый боул.'])
oven=[('Курица с картофелем','chicken:150 potato:180 carrot:80'),('Индейка с бататом','turkey:150 sweet_potato:180 pepper:80'),('Курица с брокколи','chicken:150 broccoli:180 yogurt:40'),('Курица с тыквой','chicken:150 pumpkin:200 onion:40'),('Индейка с кабачком','turkey:150 zucchini:200 tomato:80'),('Курица с цветной капустой','chicken:150 cauliflower:200 cheese:20'),('Индейка с грибами','turkey:150 mushroom:150 onion:40 yogurt:40'),('Курица с баклажаном','chicken:150 eggplant:150 tomato:100'),('Индейка с морковью и картофелем','turkey:150 carrot:100 potato:150'),('Курица с перцем и томатами','chicken:150 pepper:150 tomato:100')]
for title,extra in oven:
    add(title,'poultry',50,390,'olive_oil:7 '+extra,['Разогрейте духовку до 190 °C. Нарежьте твёрдые овощи небольшими кусочками, а филе — одинаковыми порционными кусками.','Смешайте овощи с маслом в форме. Птицу подготовьте на отдельной доске; распределите поверх овощей. Йогурт, если он указан, используйте как покрытие для филе.','Запекайте около 30–40 минут, ориентируясь на размер кусочков и особенности духовки. Проверяйте готовность птицы пищевым термометром по инструкции к продукту; при необходимости увеличьте время.','Сыр добавьте в последние минуты, если он входит в состав. Подавайте, убедившись, что птица полностью приготовлена, а овощи мягкие.'])
fish=[('Лосось с брокколи','salmon:150 broccoli:180'),('Треска с картофелем','cod:170 potato:180'),('Лосось с кабачком','salmon:150 zucchini:200'),('Треска с томатами','cod:170 tomato:160 onion:40'),('Лосось с цветной капустой','salmon:150 cauliflower:180'),('Треска с морковью','cod:170 carrot:150 onion:40'),('Лосось с бататом','salmon:150 sweet_potato:180'),('Треска с перцем','cod:170 pepper:160 tomato:80'),('Лосось со шпинатом','salmon:150 spinach:120 yogurt:40'),('Треска с тыквой','cod:170 pumpkin:200 onion:30')]
for title,extra in fish:
    add(title,'fish',40,340,'olive_oil:5 lemon:15 '+extra,['Разогрейте духовку до 190 °C. Удалите из рыбы кости. Овощи нарежьте небольшими кусочками.','Картофель, батат, морковь или тыкву предварительно запекайте с частью масла до полуготовности. Мягкие овощи можно положить сразу с рыбой.','Разместите рыбу поверх овощей, добавьте оставшееся масло и лимон. Йогурт, если он указан, распределите по рыбе.','Запекайте примерно 15–25 минут после добавления рыбы, до полной готовности. Время зависит от толщины филе; при необходимости готовьте дольше.'])
plants=[('Нут с овощами в томате','chickpeas:180 zucchini:100 tomato_passata:100'),('Чечевица с грибами','lentils:180 mushroom:150 onion:40'),('Фасоль с перцем','beans:180 pepper:150 tomato_passata:100'),('Тофу с брокколи','tofu:180 broccoli:180 soy_sauce:10'),('Нут с тыквой','chickpeas:160 pumpkin:180 coconut_milk:40'),('Чечевица со шпинатом','lentils:180 spinach:100 tomato:100'),('Фасоль с баклажаном','beans:160 eggplant:180 tomato_passata:100'),('Тофу с кабачком и морковью','tofu:180 zucchini:150 carrot:70'),('Горошек с цветной капустой','peas:150 cauliflower:200 yogurt:50'),('Нут с грибами и перцем','chickpeas:160 mushroom:120 pepper:100')]
for title,extra in plants:
    add(title,'plant',30,400,'olive_oil:7 '+extra,['Подготовьте овощи: вымойте, очистите при необходимости и нарежьте. Бобовые в этом рецепте уже отваренные.','Разогрейте масло в сотейнике, потушите овощи с небольшим количеством воды до мягкости. Грибы сначала прогрейте до испарения жидкости.','Добавьте бобовые или тофу и оставшиеся ингредиенты соуса согласно составу. Аккуратно перемешайте.','Готовьте на слабом огне ещё 5–10 минут. Молочный йогурт добавляйте в конце, не допуская сильного кипения.'])
toasts=[('Тост с авокадо и яйцом','avocado:70 egg_cooked:50 tomato:50'),('Тост с творогом и огурцом','cottage:100 cucumber:100 herbs:5'),('Тост с тунцом','tuna:100 yogurt:30 cucumber:60'),('Тост с курицей','chicken_cooked:100 tomato:80 lettuce:20'),('Тост с моцареллой','mozzarella:60 tomato:100 herbs:5'),('Тост с нутовой пастой','chickpeas:130 lemon:10 olive_oil:5'),('Тост с фасолевой пастой','beans:130 tomato:80 olive_oil:5'),('Тост с индейкой','turkey_cooked:100 avocado:40 lettuce:20'),('Тост с рикоттой и грушей','ricotta:80 pear:100 walnut:10'),('Тост с бананом и арахисом','banana:100 peanut:20 yogurt:40')]
for title,extra in toasts:
    add(title,'snack',10,280,'bread:70 '+extra,['Подсушите хлеб на сухой сковороде или в тостере. Свежие ингредиенты вымойте и нарежьте.','Разомните авокадо или отваренные бобовые вилкой; смешайте с указанными маслом, лимоном или йогуртом. Орехи измельчите.','Распределите намазку и оставшуюся начинку по хлебу. Птица и яйца в списке уже приготовленные; тунец — консервированный.','Подавайте сразу, чтобы хлеб сохранил текстуру.'])
smoothies=[('Смузи с бананом и ягодами','banana:100 berries:100 kefir:200'),('Смузи с манго','mango:150 yogurt:150 water:100'),('Смузи с грушей и овсянкой','pear:130 oats:25 kefir:200'),('Смузи с киви и шпинатом','kiwi:120 spinach:30 banana:80 water:150'),('Клубничный смузи','strawberry:180 greek_yogurt:150 milk:100'),('Шоколадный смузи','banana:100 cocoa:7 milk:220 oats:20'),('Смузи с персиком','peach:160 yogurt:150 water:80'),('Смузи с ананасом','pineapple:160 coconut_milk:80 water:120'),('Черничный смузи','blueberry:120 soy_milk:200 banana:70'),('Яблочно-морковный смузи','apple:120 carrot:60 orange:100 water:150')]
for title,ing in smoothies:
    add(title,'drink',10,int(sum(float(x.split(':')[1]) for x in ing.split())),ing,['Вымойте фрукты, ягоды и овощи. Удалите косточки, несъедобную кожуру и сердцевину. Морковь нарежьте мелко.','Если в составе есть овсяные хлопья, используйте готовые к употреблению хлопья или предварительно приготовьте их по упаковке и остудите.','Сложите ингредиенты в блендер и измельчите до однородности. При необходимости добавьте немного воды.','Перелейте в стакан и подавайте сразу. Пищевая ценность учитывает весь состав, а не только жидкую часть.'])
desserts=[('Творожный стакан с ягодами','cottage:150 yogurt:60 berries:100 honey:10'),('Йогурт с манго и чиа','greek_yogurt:180 mango:100 chia:10'),('Творог с бананом и какао','cottage:150 banana:100 cocoa:5 yogurt:40'),('Йогурт с яблоком и орехами','yogurt:180 apple:120 walnut:15'),('Рикотта с персиком','ricotta:120 peach:150 honey:10'),('Творог с курагой','cottage:150 apricot_dried:30 yogurt:60'),('Йогурт с грушей и миндалём','greek_yogurt:160 pear:120 almond:15'),('Банановый стакан с арахисом','yogurt:160 banana:100 peanut:20'),('Творог с черникой','cottage:150 blueberry:100 yogurt:50'),('Йогурт с финиками и какао','yogurt:180 dates:30 cocoa:5 walnut:10')]
for title,ing in desserts:
    add(title,'dessert',10,int(sum(float(x.split(':')[1]) for x in ing.split())),ing,['Вымойте фрукты и ягоды. Из фиников удалите косточки; сухофрукты промойте, жёсткие предварительно размочите.','Размешайте молочную основу до однородности. Какао и мёд, если они есть в составе, добавьте в основу.','Выложите основу и нарезанные фрукты слоями. Добавьте измельчённые орехи или семена согласно составу.','Вариант с чиа оставьте в холодильнике на 20 минут; остальные можно подавать сразу.'])
assert len(recipes)==120 and len({r['title'] for r in recipes})==120
assert len(products)==100
for name,data in [('products',products),('recipes',recipes)]:
    (ASSETS/(name+'.json')).write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
print(f'Catalog: {len(recipes)} recipes, {len(products)} products')
