import sys
from pathlib import Path

try:
    import qrcode
except ImportError:
    print("Install first: pip install qrcode[pil]")
    raise

payload = sys.stdin.read().strip()

if not payload:
    raise SystemExit("Paste QR text into stdin.")

qr = qrcode.QRCode(
    error_correction=qrcode.constants.ERROR_CORRECT_M,
    box_size=10,
    border=4,
)
qr.add_data(payload)
qr.make(fit=True)

img = qr.make_image(fill_color="black", back_color="white")
out = Path("whatsapp-qr.png")
img.save(out)

print(f"Wrote {out.resolve()}")
