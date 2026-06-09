This mod allows you to add frequently used commands to bookmarks/favorites in REI.

To add a command, simply type the commands in the search bar (starting and separated by "/") and press enter.

To name a bookmark, type some words before the "/" when adding the bookmark. (can use "$" for formatting codes)

## Full feature list:

<details>
<summary>1.21+</summary>

Text component parsing is supported. You can use it to add hex color or localizable text. For example:

```
{text:HI,color:"#FFAA00"}/say hi
```

(In this example, you can see the [lenient JSON mode](https://futurestud.io/tutorials/gson-builder-relax-gson-with-lenient) is enabled. So you don't have to always enter quotes.)

You can also specify an item as an icon. For example:

```
{item:"minecraft:apple"}/say hi
```

Alternatively, to assign tags to the item:

```
{item:"potion[potion_contents={potion:\\"minecraft:regeneration\\"}]"}/say hi
```

A small command block will be displayed on the icon. Holding CTRL to hide it.

When adding a custom command, press CTRL+Enter to keep the contents in the search box.

Multiple commands: Separated by "/", escaped by "\\"

```
/tellraw @a "Note about \\"REICC\\":" /say You can download it from CurseForge\/Modrinth
```

</details>

<details>
<summary>1.19-1.20</summary>

Text component parsing is supported. You can use it to add hex color or localizable text. For example:

```
{text:HI,color:"#FFAA00"}/say hi
```

(In this example, you can see the [lenient JSON mode](https://futurestud.io/tutorials/gson-builder-relax-gson-with-lenient) is enabled. So you don't have to always enter quotes.)

You can also specify an item as an icon. For example:

```
{item:"minecraft:apple"}/say hi
```

Alternatively, to assign tags to the item:

```
{item:{id:potion,tag:{Potion:regeneration}}}/say hi
```

A small command block will be displayed on the icon. Holding CTRL to hide it.

When adding a custom command, press CTRL+Enter to keep the contents in the search box.

### New in 1.20.1+: Multiple Commands

Separated by "/", escaped by "\\"

```
/tellraw @a "Note about \\"REICC\\":" /say You can download it from CurseForge\/Modrinth
```

</details>

[![Interested in supporting me and the development of mods? BisectHosting is the perfect solution. New customers can use the promotion code "snownee" to get a 25% discount on their first month of a gaming server. With 24/7 support and fast response times, you can expect top-notch service for all your gaming needs.](https://www.bisecthosting.com/partners/custom-banners/e826c64b-9e14-433d-a083-26f659f8a41c.webp)](https://rb.gy/sscocj)