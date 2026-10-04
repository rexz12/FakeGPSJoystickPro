# Fake GPS Pro 3.0

Upgrade utama:
- OSM map.
- Transparent joystick.
- Coordinate input.
- Multiple waypoints.
- BUILD ROUTE menggunakan OSRM.
- Rute jalan (driving) dari posisi sekarang -> waypoint 1 -> waypoint 2 -> dst.
- AUTO benar-benar mengikuti polyline jalan yang dikembalikan routing server.
- Kecepatan 1-120 km/jam.
- Update simulasi 100 ms.
- Mock GPS Android.

Cara:
1. Import project ke Android Studio.
2. Build/install.
3. Android Developer Options -> Select mock location app -> Fake GPS Pro.
4. SET posisi awal.
5. Masukkan koordinat waypoint -> + WAYPOINT.
6. BUILD ROUTE.
7. START AUTO.

Catatan routing:
Aplikasi memakai endpoint demo OSRM publik. Untuk penggunaan serius, sebaiknya gunakan server routing sendiri karena endpoint publik memiliki kebijakan dan batas penggunaan.

Mode manual:
Joystick menggerakkan posisi secara langsung. Mode AUTO mengikuti titik-titik rute jalan yang diperoleh OSRM.

Batasan:
- Ini simulator/mock location, bukan pengubah GPS hardware.
- Aplikasi/layanan yang mendeteksi mock location tetap dapat menolak lokasi tersebut.
