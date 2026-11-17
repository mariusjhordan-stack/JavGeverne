# Image sources

Every photograph on the storefront was downloaded from **Wikimedia Commons** on 2026-11-17 and is stored in
`frontend/public/images/`. Nothing is hot-linked, no placeholder service is used, and no image comes from a
search-engine result. Licence and author are those shown on each file's Commons page at download time.

Images were converted to WebP and cropped (never retouched). Each Commons file is the **original**; the files in this
repository are derivatives, so the licence's attribution and share-alike terms apply to them (CC BY / CC BY-SA files stay
under the same licence; CC0 and public-domain files carry no condition).

## Important limits

* The photographs are **representative**, not catalogue shots of the exact reference. The Seamaster 300M picture is a
  chronograph version; the Constellation is a vintage 1958 piece; the Monaco was photographed in a showroom;
  the Submariner photographs are amateur quality. The product page says so. They should be replaced with the retailer's own
  photography for a real shop.
* Showing third-party brand names and products is nominative use inside an academic demonstration. Cronos Genève is not
  affiliated with Rolex, Omega or TAG Heuer, and the site says so in its footer and on the maison pages.
* No image was published with a watermark. Photographs of people are limited to a wrist or forearm.

## Register

| Source file (Wikimedia Commons) | Author | Licence | Local derivative | Use |
|---|---|---|---|---|
| [Rolex Cosmograph Daytona Panda.jpg](https://commons.wikimedia.org/wiki/File:Rolex_Cosmograph_Daytona_Panda.jpg) | Shuntfield | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | `products/rolex-daytona*.webp`, `editorial/collection-sport.webp`, `editorial/craft-detail.webp` | Daytona product and gallery; "Sport" collection; craftsmanship |
| [Rolex Datejust 126234.jpg](https://commons.wikimedia.org/wiki/File:Rolex_Datejust_126234.jpg) | ajay_suresh | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | `products/rolex-datejust-41*.webp`, `editorial/collection-dress.webp`, `editorial/craft.webp` | Datejust product and gallery; "Dress" collection; craftsmanship |
| [Rolex GMT Master II 126710BLRO.jpg](https://commons.wikimedia.org/wiki/File:Rolex_GMT_Master_II_126710BLRO.jpg) | Qubeek | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | `products/rolex-gmt-master-ii*.webp`, `editorial/hero.webp` | GMT-Master II product and gallery; home hero |
| [Rolex Oyster Perpetual Date Submariner Watch.JPG](https://commons.wikimedia.org/wiki/File:Rolex_Oyster_Perpetual_Date_Submariner_Watch.JPG) | Eternalsleeper | Public domain (as declared on Commons) | `products/rolex-submariner-date.webp`, `…-detail.webp` | Submariner product and gallery |
| [Rolex Submariner watch 16613.JPG](https://commons.wikimedia.org/wiki/File:Rolex_Submariner_watch_16613.JPG) | Eternalsleeper | Public domain (as declared on Commons) | `products/rolex-submariner-date-bracelet.webp` | Submariner gallery |
| [Speedmaster (23456608783).jpg](https://commons.wikimedia.org/wiki/File:Speedmaster_(23456608783).jpg) | Daniel Zimmermann | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | `products/omega-speedmaster-moonwatch*.webp`, `editorial/collection-chronograph.webp` | Speedmaster product and gallery; "Chronograph" collection |
| [Omega Seamaster Diver chronograph watch (300 m water resistance).jpg](https://commons.wikimedia.org/wiki/File:Omega_Seamaster_Diver_chronograph_watch_(300_m_water_resistance).jpg) | Thomas Quine | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0) | `products/omega-seamaster-300m*.webp`, `editorial/collection-dive.webp` | Seamaster product and gallery; "Dive" collection |
| [Omega Constellation Rotgold 1958.jpg](https://commons.wikimedia.org/wiki/File:Omega_Constellation_Rotgold_1958.jpg) | Noop1958 | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | `products/omega-constellation*.webp`, `editorial/heritage.webp` | Constellation product and gallery; heritage |
| [Omega Seamaster Planet Ocean (Referenz 215.30.44.21.01.001).jpg](https://commons.wikimedia.org/wiki/File:Omega_Seamaster_Planet_Ocean_(Referenz_215.30.44.21.01.001).jpg) | KaiKnight2 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | `products/omega-seamaster-planet-ocean*.webp`, `editorial/lifestyle.webp` | Planet Ocean product and gallery; lifestyle |
| [TAG Heuer Carrera chronograph CV2010.JPG](https://commons.wikimedia.org/wiki/File:TAG_Heuer_Carrera_chronograph_CV2010.JPG) | Pinback66 | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | `products/tag-heuer-carrera-chronograph*.webp` | Carrera product and gallery |
| [TAG Heuer Monaco 40th Anniversary re-edition.JPG](https://commons.wikimedia.org/wiki/File:TAG_Heuer_Monaco_40th_Anniversary_re-edition.JPG) | Ferengi | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) | `products/tag-heuer-monaco*.webp` | Monaco product and gallery; TAG Heuer maison |
| [TAG Heuer Aquaracer tribute to ref. 844.jpg](https://commons.wikimedia.org/wiki/File:TAG_Heuer_Aquaracer_tribute_to_ref._844.jpg) | Clyde94 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | `products/tag-heuer-aquaracer-professional-300*.webp` | Aquaracer product and gallery |
| [Tag Heuer Formula 1 Calibre 5 Automatic (WAZ2011.BA0842).jpg](https://commons.wikimedia.org/wiki/File:Tag_Heuer_Formula_1_Calibre_5_Automatic_(WAZ2011.BA0842).jpg) | 사나운 | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0) | `products/tag-heuer-formula-1*.webp` | Formula 1 product and gallery |

`…-detail.webp` files are tighter crops of the same photograph, used as the second gallery image.

## Not images

Typefaces (Cormorant Garamond, Inter) are loaded from Google Fonts under the SIL Open Font License. Icons come from
`lucide-react` (ISC licence). The favicon is the project's own SVG.

## Adding or replacing an image

1. Prefer your own photography. Otherwise use Wikimedia Commons and check the licence on the file page.
2. Download it, convert to WebP (about 1000 px wide for products) and place it under `frontend/public/images/`.
3. Add a row to the table above (source link, author, licence, derivative, use) in the same commit.
4. Point the product at it from the administration panel (Products → Edit → image paths).
