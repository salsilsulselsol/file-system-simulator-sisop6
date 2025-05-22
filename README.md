# Simulator Sistem File Unix Sederhana (dengan GUI Swing)

---

## Daftar Isi
* [Gambaran Umum](#gambaran-umum)
* [Fitur](#fitur)
* [Panduan Instalasi & Menjalankan](#instalasi)
  * [Prasyarat](#prasyarat)
  * [Langkah-Langkah](#langkah-langkah)
* [Cara Kerja](#cara-kerja)
  * [Antarmuka Pengguna Grafis (GUI)](#gui)
  * [Struktur File Sistem Internal](#struktur-file)
    * [Super Block](#super-block)
    * [Bitmap](#bitmap)
    * [Index Node (inode)](#index-node)
    * [Blok Data](#blok-data)
  * [Segmentasi Disk (File Kontainer)](#segmentasi-disk)
* [Daftar Perintah (via GUI)](#daftar-perintah)
* [Rencana Pengembangan Selanjutnya](#rencana-pengembangan)
* [Kontribusi](#kontribusi)

---

<a name="gambaran-umum"></a>
## Gambaran Umum
Proyek ini adalah simulator sistem file bergaya Unix yang ditulis dalam Java 17. Awalnya dirancang sebagai aplikasi baris perintah (CLI), proyek ini telah dikembangkan untuk menyertakan Antarmuka Pengguna Grafis (GUI) menggunakan Java Swing, sehingga memberikan pengalaman pengguna yang lebih interaktif dan visual.

Simulator ini mengimplementasikan beberapa struktur data dasar yang ditemukan dalam sistem file nyata, seperti:
- Super Block
- Bitmap untuk manajemen blok
- Index Node (inode) untuk metadata file
- Blok Data untuk menyimpan konten file aktual

Sistem file simulasi ini beroperasi dalam satu file biner tunggal yang berfungsi sebagai "kontainer" atau "disk virtual".

<a name="fitur"></a>
## Fitur
* **Simulasi Struktur File Sistem:** Menggunakan Super Block, inode, bitmap, dan blok data.
* **Manajemen File dan Direktori:**
    * Membuat direktori (`mkdir`)
    * Menghapus direktori (saat ini) (`rmdir`)
    * Mendaftar isi direktori (output ke area log) (`ls`)
    * Berpindah direktori (`cd`)
    * Menyalin file (`cp`)
    * Menghapus file (`rm`)
    * Menampilkan konten file (`cat` ke area tampilan khusus)
    * Menulis ke file baru atau menimpa file (`write`)
    * Menambahkan konten ke akhir file (`write +append`)
* **Interaksi dengan Sistem File Host:**
    * Mengimpor file dari sistem file host ke dalam simulator (`import`)
    * Mengekspor file dari simulator ke sistem file host (`export`)
* **Antarmuka Pengguna Grafis (GUI):**
    * Dibangun menggunakan Java Swing.
    * Visualisasi struktur direktori menggunakan `JTree`.
    * Area khusus untuk menampilkan konten file.
    * Tombol-tombol intuitif untuk setiap operasi sistem file.
    * Area log untuk menampilkan pesan status dan output perintah.
    * Dialog interaktif untuk input pengguna.
* **Inisialisasi Fleksibel:** Pengguna menentukan path ke file kontainer dan ukuran maksimum sistem file saat startup.

---

<a name="instalasi"></a>
## Panduan Instalasi & Menjalankan

<a name="prasyarat"></a>
### Prasyarat
* Java Development Kit (JDK) 17 atau lebih tinggi.
* Apache Maven (untuk membangun proyek dari source code).
* Git (opsional, jika Anda ingin meng-clone repositori).

<a name="langkah-langkah"></a>
### Langkah-Langkah

1.  **Dapatkan Kode Sumber:**
    * **Opsi A: Unduh ZIP**
        * Kunjungi repositori GitHub proyek ini.
        * Klik tombol "Code" lalu "Download ZIP".
        * Ekstrak file ZIP ke direktori pilihan Anda.
    * **Opsi B: Clone Repositori (jika menggunakan Git)**
        ```bash
        git clone [URL_REPOSITORI_ANDA]
        cd [NAMA_DIREKTORI_PROYEK]
        ```

2.  **Bangun Proyek dengan Maven:**
    Buka terminal atau command prompt di direktori root proyek.
    * Jika Anda memiliki Maven terinstal secara global:
        ```bash
        mvn clean install
        ```
    * Jika Anda menggunakan Maven Wrapper yang disertakan (direkomendasikan jika Anda tidak yakin dengan versi Maven Anda):
        * Untuk Windows: `mvnw.cmd clean install`
        * Untuk Linux/macOS: `./mvnw clean install`

3.  **Jalankan Aplikasi:**
    * Setelah proyek berhasil dibangun, Anda dapat menjalankannya melalui IDE Java Anda (misalnya IntelliJ IDEA, Eclipse):
        * Impor proyek sebagai proyek Maven.
        * Temukan dan jalankan kelas `com.yoanpetrov.filesystemsimulator.Main`.
    * Alternatifnya, Anda dapat menjalankan file `.jar` yang telah dihasilkan (jika `pom.xml` dikonfigurasi untuk membuat *executable JAR*). Periksa folder `target` setelah build.

4.  **Inisialisasi Sistem File (Saat Startup GUI):**
    * Saat aplikasi GUI pertama kali berjalan, sebuah dialog akan muncul meminta Anda untuk:
        * **Path ke file kontainer:** Tentukan lokasi dan nama file tempat sistem file simulasi akan disimpan (misalnya, `C:\SimFS\myDisk.dat` atau `/home/user/SimFS/myDisk.dat`). File ini tidak perlu ada sebelumnya.
        * **Ukuran maksimum sistem file (dalam byte):** Masukkan ukuran yang diinginkan (misalnya, `262144` untuk 256KB). Disarankan menggunakan kelipatan dari ukuran blok (512 byte) atau pangkat 2.
    * Klik "OK" untuk melanjutkan.

---

<a name="cara-kerja"></a>
## Cara Kerja

<a name="gui"></a>
### Antarmuka Pengguna Grafis (GUI)
Aplikasi ini sekarang menggunakan antarmuka berbasis Swing yang terdiri dari beberapa komponen utama:
* **Tree View (JTree):** Menampilkan struktur hierarki direktori dan file dalam sistem file simulasi secara visual. Pengguna dapat melihat bagaimana file dan direktori diorganisir.
* **Area Konten File:** Sebuah area teks khusus untuk menampilkan isi dari file yang dipilih atau yang kontennya diminta melalui perintah `cat`.
* **Panel Tombol Perintah:** Kumpulan tombol yang masing-masing merepresentasikan operasi sistem file (misalnya, `mkdir`, `cd`, `ls`, `write`). Ini menggantikan kebutuhan untuk mengetik perintah secara manual.
* **Label Path Saat Ini:** Menunjukkan path direktori aktif di dalam sistem file simulasi.
* **Area Log Output:** Menampilkan pesan status, konfirmasi operasi, output dari perintah seperti `ls`, dan pesan error.
* **Dialog Interaktif:** `JOptionPane` digunakan untuk mendapatkan input dari pengguna untuk berbagai perintah (misalnya, nama file, path, konten).

<a name="struktur-file"></a>
### Struktur File Sistem Internal
Implementasi ini menggunakan satu file biner sebagai "disk" atau kontainer untuk keseluruhan sistem file. File ini dibagi menjadi beberapa blok dengan ukuran tetap (default 512 byte).

<a name="super-block"></a>
#### Super Block
Blok pertama dalam kontainer, berisi metadata penting tentang sistem file, seperti:
* Ukuran blok.
* Jumlah total blok.
* Ukuran maksimum sistem file (dalam byte).
* Offset (posisi awal) untuk region bitmap dan region inode/data.

<a name="bitmap"></a>
#### Bitmap
Digunakan untuk melacak status alokasi blok-blok di region inode dan region data. Satu bit mewakili satu blok; misalnya, 0 bisa berarti blok bebas dan 1 berarti blok terisi.

<a name="index-node"></a>
#### Index Node (inode)
Setiap file dan direktori direpresentasikan oleh sebuah inode. Inode menyimpan metadata tentang file/direktori tersebut, termasuk:
* Tipe (file atau direktori).
* Ukuran file.
* Nama file/direktori (hingga 16 karakter).
* Pointer ke blok-blok data yang menyimpan konten aktual file (menggunakan *direct blocks*). Implementasi saat ini memungkinkan hingga 56 blok data per file, menghasilkan ukuran file maksimum sekitar 28KB.

<a name="blok-data"></a>
#### Blok Data
Blok-blok ini menyimpan konten mentah dari file. Inode menunjuk ke blok-blok data ini.

<a name="segmentasi-disk"></a>
### Segmentasi Disk (File Kontainer)
Saat sistem file dibuat, file kontainer dibagi menjadi beberapa region berdasarkan perhitungan offset yang disimpan di Super Block:
1.  Region Super Block
2.  Region Bitmap Inode
3.  Region Bitmap Blok Data
4.  Region Inode
5.  Region Blok Data

---

<a name="daftar-perintah"></a>
## Daftar Perintah (via GUI)
Semua perintah diakses melalui tombol-tombol pada antarmuka pengguna. Dialog input akan muncul untuk meminta argumen yang diperlukan.

* **`mkdir`**: Membuat direktori baru. Pengguna akan diminta memasukkan nama direktori.
* **`rmdir`**: Menghapus direktori *saat ini* jika direktori tersebut kosong. Pengguna akan diminta konfirmasi.
* **`ls (to log)`**: Menampilkan daftar konten dari direktori saat ini ke area log.
* **`cd`**: Mengubah direktori saat ini. Pengguna akan diminta memasukkan path tujuan (misalnya, `nama_direktori`, `..` untuk ke induk, `/` untuk ke root).
* **`cp`**: Menyalin file. Pengguna akan diminta memasukkan nama file sumber dan nama file tujuan.
* **`rm`**: Menghapus file. Pengguna akan diminta memasukkan nama file dan konfirmasi.
* **`cat (to view)`**: Menampilkan konten file ke area tampilan konten khusus. Pengguna akan diminta memasukkan nama file. File juga dapat ditampilkan dengan memilihnya di Tree View.
* **`write`**: Menulis konten ke file. Pengguna akan diminta nama file dan kontennya. Ada opsi untuk `+append` (menambahkan ke akhir file) atau menimpa (default).
* **`import`**: Mengimpor file dari sistem file komputer host ke dalam simulator. Pengguna akan memilih file eksternal dan menentukan nama file tujuan di simulator.
* **`export`**: Mengekspor file dari simulator ke sistem file komputer host. Pengguna akan memilih file di simulator dan menentukan path tujuan di komputer host.
* **`Help`**: Menampilkan dialog bantuan yang berisi daftar penggunaan perintah ini.
