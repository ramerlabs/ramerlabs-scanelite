# RamerLabs store product playbook

Use this when publishing a new licensed product on **ramerlabs.com**.

Machine-readable twin: `C:\Users\Lacida\Projects\ramerlabs-store-product-playbook.json`  
Latest example: `C:\Users\Lacida\Projects\scanelite\config\store-product.scanelite.json`

## Checklist

1. **Category** — create/assign (e.g. `Mobile App`, `Wordpress Plugin`, `Script`)
2. **Create WooCommerce product** — simple, virtual, published, price set
3. **Featured image** — upload brand logo/hero via `POST /wp-json/wp/v2/media`, attach to product
4. **Description** — ad-style HTML **~600 words** (problem → product → features → how to activate → CTA → RamerLabs credits)
5. **Short description** — 1–2 benefit sentences + license mention
6. **Tags** — 5–12 discovery tags
7. **Link License Manager** — `admin/link-store-product`
8. **Create license key(s)** — `admin/create-license` (save outside git)
9. **Gate the app** — activation UI only; never expose LM server URL to customers
10. **Public repo** (if releasing publicly) — README with image + buy URL + ramerlabs.com credits

## Content rules

- Buy link = product permalink on ramerlabs.com  
- Credits always include **RamerLabs** / **ramerlabs.com**  
- Keep internal LM base URL out of UI, errors, and plugin headers  

## WordPress auth

Credentials file: `D:\cursor\blog_application_password.txt`  
(`site=`, `username=`, `application_password=`)

## ScanElite reference

- Product: https://ramerlabs.com/product/scanelite/  
- Category: Mobile App  
- Image: brand logo uploaded as featured media  
- Copy files: `scanelite/config/store-description.html`, `store-short-description.html`
