"""Validate the distributable SQLite catalog with Python's standard library."""
from pathlib import Path
import re
import sqlite3

root = Path(__file__).resolve().parents[1]
path = root / 'app/src/main/assets/catalog.db'
with sqlite3.connect(path.as_uri() + '?mode=ro', uri=True) as db:
    assert db.execute('PRAGMA integrity_check').fetchone()[0] == 'ok'
    assert db.execute('PRAGMA foreign_key_check').fetchall() == []
    assert db.execute('PRAGMA user_version').fetchone()[0] == 3
    assert db.execute('SELECT product,COUNT(*) FROM catalog_items GROUP BY product').fetchall() == [(0,120),(1,100)]
    categories = db.execute('SELECT category,COUNT(*) FROM catalog_items WHERE product=0 GROUP BY category').fetchall()
    assert len(categories) == 12 and all(count == 10 for _, count in categories)
    assert db.execute('SELECT COUNT(DISTINCT title) FROM catalog_items').fetchone()[0] == 220
    for table in ('recipe_ingredients', 'recipe_steps'):
        counts = db.execute(f'SELECT recipe_id,COUNT(*),MIN(position),MAX(position) FROM {table} GROUP BY recipe_id').fetchall()
        assert len(counts) == 120
        assert all(count >= 3 and first == 0 and last == count-1 for _,count,first,last in counts)
    assert not db.execute('SELECT i.product_id FROM recipe_ingredients i JOIN catalog_items p ON p.id=i.product_id WHERE p.product<>1').fetchall()
    for ident, kcal in db.execute('SELECT id,kcal FROM catalog_items WHERE product=0'):
        expected = db.execute('SELECT SUM(p.kcal*i.grams/100) FROM recipe_ingredients i JOIN catalog_items p ON p.id=i.product_id WHERE i.recipe_id=?',(ident,)).fetchone()[0]
        assert abs(expected-kcal) <= .051, ident
        expected_allergens = set(db.execute('SELECT DISTINCT a.allergen FROM recipe_ingredients i JOIN item_allergens a ON a.item_id=i.product_id WHERE i.recipe_id=?',(ident,)))
        actual = set(db.execute('SELECT allergen FROM item_allergens WHERE item_id=?',(ident,)))
        assert expected_allergens == actual, ident
    for source,en,pl in db.execute('SELECT source,en,pl FROM catalog_text'):
        if source:
            assert en and pl
            assert not re.search('[А-Яа-яЁё]',en+pl), source
    assert db.execute('SELECT COUNT(*) FROM lessons').fetchone()[0] == 14
    videos = db.execute("SELECT url,premium,author FROM lessons WHERE url<>''").fetchall()
    assert len(videos) == 8 and all(url.startswith('https://') and not premium and author for url,premium,author in videos)
    assert not db.execute("SELECT name FROM sqlite_master WHERE name IN ('diary','local_account','profile')").fetchall()
    for (table,) in db.execute("SELECT name FROM sqlite_master WHERE type='table'"):
        assert 'payload' not in [c[1] for c in db.execute(f'PRAGMA table_info({table})')]
    assert not list((root/'app/src/main/assets').rglob('*.json'))
print('SQLite OK: 120 recipes, 100 products, 14 lessons, RU/EN/PL, integrity, relations, nutrition, no JSON.')
