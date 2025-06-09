## Simulator Sistem Manajemen File Unix Sederhana (dengan JAVA GUI Swing)

### Daftar Isi

- Gambaran Umum
- Fitur
- Panduan Instalasi & Menjalankan
- Cara Kerja
- Daftar Perintah (via GUI)
- Rencana Pengembangan Selanjutnya
- Kontribusi

---

## Gambaran Umum

Simulator ini adalah aplikasi sistem file bergaya Unix yang ditulis dalam Java 17. Awalnya berbasis CLI, kini telah dikembangkan dengan antarmuka grafis (GUI) menggunakan Java Swing agar lebih interaktif dan mudah digunakan. Simulator ini mengimplementasikan struktur data utama yang biasa ditemukan pada sistem file nyata, seperti:

- **Super Block** (metadata utama sistem file)
- **Bitmap** (manajemen blok)
- **Index Node (inode)** (metadata file/direktori)
- **Blok Data** (penyimpanan konten file)

Seluruh sistem file disimpan dalam satu file biner yang berperan sebagai "disk virtual" atau kontainer.

---

## Fitur

- **Simulasi Struktur File Sistem**: Menggunakan Super Block, inode, bitmap, dan blok data.
- **Manajemen File & Direktori**:
  - Membuat direktori (`mkdir`)
  - Menghapus direktori kosong (`rmdir`)
  - Melihat isi direktori (`ls`)
  - Berpindah direktori (`cd`)
  - Menyalin file (`cp`)
  - Menghapus file (`rm`)
  - Menampilkan isi file (`cat`)
  - Menulis atau menambah konten file (`write`, `write +append`)
- **Interaksi dengan Sistem File Host**:
  - Mengimpor file dari host (`import`)
  - Mengekspor file ke host (`export`)
- **Antarmuka Pengguna Grafis (GUI)**:
  - Visualisasi struktur direktori dengan `JTree`
  - Area tampilan konten file
  - Tombol operasi file sistem
  - Area log untuk status/output
  - Dialog interaktif untuk input pengguna
- **Inisialisasi Fleksibel**: Pengguna menentukan path file kontainer dan ukuran maksimum sistem file saat startup.

---

## Panduan Instalasi & Menjalankan

### Prasyarat

- Java Development Kit (JDK) 17+
- Apache Maven (atau gunakan Maven Wrapper)
- Git (opsional, untuk clone repo)

### Langkah-Langkah

**1. Dapatkan Kode Sumber**
- **Opsi A: Download ZIP**
  - Download dari GitHub, ekstrak ZIP.
- **Opsi B: Git Clone**
  - `git clone https://github.com/salsilsulselsol/file-system-simulator-sisop6.git`
  - `cd file-system-simulator-sisop6`

**2. Build Proyek**
- Jika Maven terinstal:
  - `mvn clean install`
- Jika menggunakan Maven Wrapper:
  - Windows: `mvnw.cmd clean install`
  - Linux/macOS: `./mvnw clean install`

**3. Jalankan Aplikasi**
- Dari IDE (IntelliJ/Eclipse): Jalankan kelas `filesystemsimulator.Main`
- Atau, jalankan file `.jar` di folder `target` jika tersedia

**4. Inisialisasi Sistem File**
- Saat GUI pertama kali dijalankan, masukkan:
  - Path file kontainer (misal: `C:\SimFS\myDisk.dat`)
  - Ukuran maksimum sistem file (dalam byte, misal: `262144` untuk 256KB).

---

## Cara Kerja

### Antarmuka Pengguna Grafis (GUI)

- **Tree View (`JTree`)**: Menampilkan struktur direktori/file secara hierarkis
- **Area Konten File**: Untuk menampilkan isi file (misal hasil `cat`)
- **Panel Tombol Perintah**: Untuk operasi file sistem (misal `mkdir`, `write`)
- **Label Path Saat Ini**: Menunjukkan direktori aktif
- **Area Log Output**: Status, hasil perintah, dan error
- **Dialog Interaktif**: Input nama file, path, konten, dsb melalui `JOptionPane`

### Struktur File Sistem Internal

- **Super Block**: Metadata utama (ukuran blok, jumlah blok, offset region)
- **Bitmap**: Menandai blok mana yang terpakai/kosong
- **Inode**: Metadata file/direktori (tipe, ukuran, nama, pointer ke blok data; hingga 56 blok data per file, maksimal ~28KB/file)
- **Blok Data**: Menyimpan konten file
- **Segmentasi Disk**: File kontainer dibagi region: Super Block, Bitmap Inode, Bitmap Data, Inode, Data Block.

---

## Daftar Perintah (via GUI)

Semua perintah diakses melalui tombol pada GUI, dengan dialog input jika diperlukan:

| Perintah   | Fungsi                                                                                     |
|------------|-------------------------------------------------------------------------------------------|
| mkdir      | Membuat direktori baru (input: nama direktori)                                            |
| rmdir      | Menghapus direktori kosong (input: nama direktori, konfirmasi)                            |
| ls         | Menampilkan isi direktori saat ini ke area log                                            |
| cd         | Berpindah direktori (input: path tujuan, `..` ke induk, `/` ke root)                      |
| cp         | Menyalin file (input: nama sumber & tujuan)                                               |
| rm         | Menghapus file (input: nama file, konfirmasi)                                             |
| cat        | Menampilkan isi file ke area konten (input: nama file atau pilih di Tree View)            |
| write      | Menulis/mengganti/menambah konten file (input: nama file, konten, mode append/overwrite)  |
| import     | Mengimpor file dari host ke simulator (pilih file, tentukan nama tujuan)                  |
| export     | Mengekspor file dari simulator ke host (pilih file, tentukan path tujuan)                 |
| Help       | Menampilkan dialog bantuan                                                                |

---
