# Simulator Sistem Manajemen File Unix Sederhana (dengan JAVA GUI Swing)

## Deskripsi

File System Simulator SISOP6 adalah aplikasi simulasi sistem file bertipe Unix yang dikembangkan menggunakan Java. Aplikasi ini menyediakan antarmuka grafis (GUI) berbasis Java Swing untuk memudahkan pengguna dalam melakukan berbagai operasi sistem file virtual di dalam satu file kontainer biner. Simulator ini cocok digunakan sebagai sarana pembelajaran konsep dasar sistem file, struktur data sistem file, dan implementasi operasi file pada lingkungan terisolasi.

---

## Struktur Folder dan File

- **src/main/java/**  
  Berisi kode sumber aplikasi, terdiri dari beberapa package utama:
  - **filesystem/**: Implementasi logika sistem file (SuperBlock, Bitmap, Inode, DataBlock, FileSystem, dsb).
  - **gui/**: Komponen antarmuka pengguna berbasis Swing (MainFrame, panel, dialog, dsb).
  - **command/**: Implementasi perintah-perintah sistem file (mkdir, ls, cd, cp, rm, cat, write, import, export, dsb).
  - **util/**: Kelas utilitas untuk berbagai kebutuhan umum.
  - **Main.java**: Entry point aplikasi.

- **src/main/resources/**  
  Berisi resource pendukung aplikasi (ikon, file konfigurasi, dsb).

- **pom.xml**  
  File konfigurasi Maven untuk manajemen dependensi dan build project.

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

## Panduan Penggunaan Fitur

- **Navigasi Direktori**:  
  Gunakan tree view atau tombol `cd` untuk berpindah direktori.

- **Detailed List**:  
  Klik panel/tombol "Detailed List" untuk melihat daftar isi direktori aktif beserta detail atribut file/direktori.

- **Dir Info**:  
  Klik panel/tombol "Dir Info" untuk melihat informasi metadata direktori aktif.

- **Operasi File/Direktori**:  
  Gunakan tombol perintah seperti `mkdir`, `rmdir`, `cp`, `rm`, `cat`, `write` untuk melakukan operasi file/direktori.

- **Impor/Ekspor File**:  
  Gunakan fitur import/export untuk memindahkan file antara simulator dan sistem file host.

- **Log Output**:  
  Selalu cek area log untuk mengetahui hasil setiap operasi.

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
