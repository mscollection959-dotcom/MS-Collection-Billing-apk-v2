# MS COLLECTION Billing App v2

Professional offline-first 58mm billing app for MPT-II Bluetooth ESC/POS printers.

## v2 features
- Professional home/billing/settings/history screens
- Shop name, address and GSTIN
- Shop/UPI settings
- UPI ID stored for future QR payment integration
- Discount
- Bill history (last 100)
- WhatsApp/Android share bill using the system Sharesheet
- MPT-II Bluetooth printer connection and test print
- 58mm ESC/POS receipt
- Bill numbers MC-00001 format
- Existing branding: MS COLLECTION / Fashion for Everyone / THANK YOU! / VISIT AGAIN ❤️
- No ads, no subscription, offline billing

## Build
Use the same GitHub Actions workflow approach from v1. Open the project in Android Studio or use the included GitHub workflow.

## Important
This v2 stores UPI ID and settings locally and provides the share/WhatsApp workflow. A true scannable UPI QR on the printed receipt requires generating a QR bitmap from the UPI URI; that is the next small print-format enhancement.
