package com.mentalys.app.utils

import com.mentalys.app.MentalysApplication
import com.mentalys.app.data.local.entity.ArticleContentEntity
import com.mentalys.app.data.local.entity.ArticleEntity
import com.mentalys.app.data.local.entity.ArticleListEntity
import com.mentalys.app.data.local.entity.FoodEntity
import com.mentalys.app.data.local.entity.SpecialistEntity

object ApiContentLocalizer {

    fun articleList(article: ArticleListEntity): ArticleListEntity {
        if (!MentalysApplication.isIndonesian()) return article
        val text = articleTranslations[article.id] ?: return article
        return article.copy(
            title = text.title,
            metadata = article.metadata?.copy(
                category = categoryId[article.metadata.category] ?: article.metadata.category,
                tags = article.metadata.tags?.map { tagId[it] ?: it },
                shortDescription = text.description
            ),
            author = article.author?.copy(
                bio = "Konten edukasi kesehatan mental yang disusun dari panduan kesehatan publik dan literatur ilmiah."
            )
        )
    }

    fun article(article: ArticleEntity): ArticleEntity {
        if (!MentalysApplication.isIndonesian()) return article
        val text = articleTranslations[article.id] ?: return article
        return article.copy(
            title = text.title,
            metadata = article.metadata?.copy(
                category = categoryId[article.metadata.category] ?: article.metadata.category,
                tags = article.metadata.tags?.map { tagId[it] ?: it },
                shortDescription = text.description
            ),
            author = article.author?.copy(
                bio = "Konten edukasi kesehatan mental yang disusun dari panduan kesehatan publik dan literatur ilmiah."
            ),
            content = localizeArticleContent(article.content, text.content)
        )
    }

    fun food(food: FoodEntity): FoodEntity {
        if (!MentalysApplication.isIndonesian()) return food
        val text = foodTranslations[food.id] ?: return food
        return food.copy(
            name = text.name,
            description = text.description,
            category = categoryId[food.category] ?: food.category
        )
    }

    fun specialist(specialist: SpecialistEntity): SpecialistEntity {
        if (!MentalysApplication.isIndonesian()) return specialist
        val text = specialistTranslations[specialist.id]
        return specialist.copy(
            title = specialistTitleId[specialist.title] ?: specialist.title,
            mainRole = text?.role ?: (specialistRoleId[specialist.mainRole] ?: specialist.mainRole),
            specializations = specialist.specializations?.map { specializationId[it] ?: it },
            aboutMe = text?.about ?: specialist.aboutMe,
            workingHours = specialist.workingHours?.map { hour ->
                hour?.copy(day = dayId[hour.day] ?: hour.day)
            }
        )
    }

    private data class ArticleText(
        val title: String,
        val description: String,
        val content: List<ArticleContentText> = emptyList()
    )
    private data class ArticleContentText(
        val text: String? = null,
        val items: List<String>? = null,
        val caption: String? = null,
        val description: String? = null
    )
    private data class FoodText(val name: String, val description: String)
    private data class SpecialistText(val role: String, val about: String)

    private fun localizeArticleContent(
        content: List<ArticleContentEntity?>,
        translations: List<ArticleContentText>
    ): List<ArticleContentEntity?> {
        if (translations.isEmpty()) return content
        return content.mapIndexed { index, block ->
            val text = translations.getOrNull(index) ?: return@mapIndexed block
            block?.copy(
                text = text.text ?: block.text,
                items = text.items ?: block.items,
                caption = text.caption ?: block.caption,
                description = text.description ?: block.description
            )
        }
    }

    private val categoryId = mapOf(
        "Mental Health Education" to "Edukasi Kesehatan Mental",
        "Self Care" to "Perawatan Diri",
        "Support Planning" to "Rencana Dukungan",
        "Safety" to "Keamanan",
        "Nutrition" to "Nutrisi",
        "Professional Help" to "Bantuan Profesional",
        "Breakfast" to "Sarapan",
        "Main Meal" to "Makanan Utama",
        "Snack" to "Camilan"
    )

    private val tagId = mapOf(
        "depression" to "depresi",
        "mental-health" to "kesehatan-mental",
        "screening" to "skrining",
        "mild" to "ringan",
        "self-care" to "perawatan-diri",
        "routine" to "rutinitas",
        "moderate" to "sedang",
        "support" to "dukungan",
        "professional-help" to "bantuan-profesional",
        "severe" to "tinggi",
        "safety" to "keamanan",
        "help" to "bantuan",
        "nutrition" to "nutrisi",
        "wellness" to "kesejahteraan",
        "sleep" to "tidur",
        "mood" to "suasana-hati",
        "anxiety" to "kecemasan",
        "family" to "keluarga",
        "communication" to "komunikasi",
        "psychiatrist" to "psikiater",
        "treatment" to "perawatan"
    )

    private val articleTranslations = mapOf(
        "depression-basics" to ArticleText(
            "Memahami Hasil Skrining Depresi",
            "Penjelasan singkat tentang arti hasil skrining dan mengapa hasil ini bukan diagnosis klinis.",
            listOf(
                ArticleContentText(
                    text = "Hasil skrining bukan diagnosis. Hasil ini adalah sinyal awal bahwa gejala seperti suasana hati yang rendah, kehilangan minat, perubahan tidur, kelelahan, sulit berkonsentrasi, perubahan nafsu makan, atau perasaan tidak berharga mungkin perlu diperhatikan lebih dekat."
                ),
                ArticleContentText(
                    text = "Depresi dapat memengaruhi perasaan, pikiran, tidur, nafsu makan, pekerjaan, studi, dan hubungan. Tenaga profesional berlisensi dapat menilai gejala, durasi, tingkat keparahan, kemungkinan penyebab medis, serta pilihan perawatan yang sesuai."
                ),
                ArticleContentText(
                    items = listOf(
                        "Gunakan skrining sebagai peringatan awal, bukan sebagai label diri.",
                        "Catat gejala, durasi, pemicu, dan dampaknya pada aktivitas harian.",
                        "Hubungi psikolog, psikiater, dokter, konselor, atau klinik jika gejala menetap atau memburuk.",
                        "Cari bantuan segera jika muncul pikiran menyakiti diri atau bunuh diri."
                    )
                )
            )
        ),
        "mild-depression-self-care" to ArticleText(
            "Risiko Depresi Ringan: Langkah Awal Praktis",
            "Rutinitas harian, kontak sosial, dan dukungan awal untuk hasil skrining risiko ringan.",
            listOf(
                ArticleContentText(
                    text = "Gejala ringan tetap perlu diperhatikan, terutama jika berlangsung selama beberapa minggu atau mulai memengaruhi studi, pekerjaan, hubungan, atau perawatan diri."
                ),
                ArticleContentText(
                    items = listOf(
                        "Pertahankan jadwal tidur dan bangun yang konsisten.",
                        "Rencanakan satu aktivitas kecil yang realistis setiap hari.",
                        "Hindari menarik diri sepenuhnya dari orang yang dipercaya.",
                        "Pantau apakah gejala membaik, tetap sama, atau memburuk."
                    )
                )
            )
        ),
        "moderate-depression-support-plan" to ArticleText(
            "Risiko Depresi Sedang: Kapan Perlu Menambah Dukungan",
            "Rencana dukungan untuk hasil skrining yang menunjukkan gejala mulai memengaruhi fungsi harian.",
            listOf(
                ArticleContentText(
                    text = "Risiko sedang dapat berarti gejala mulai lebih sulit dikelola sendiri. Dukungan profesional menjadi lebih penting ketika gejala mengganggu rutinitas, tanggung jawab, hubungan, atau rasa aman."
                ),
                ArticleContentText(
                    items = listOf(
                        "Bagikan hasil ini kepada orang tepercaya jika terasa aman.",
                        "Siapkan catatan tentang durasi gejala, tidur, nafsu makan, energi, dan konsentrasi.",
                        "Gunakan riwayat Mentalys sebagai ringkasan saat berkonsultasi dengan klinisi.",
                        "Buat janji konsultasi dengan tenaga kesehatan mental atau klinik terdekat."
                    )
                )
            )
        ),
        "severe-risk-getting-help" to ArticleText(
            "Risiko Depresi Tinggi: Keamanan dan Bantuan Mendesak",
            "Langkah yang berfokus pada keamanan untuk hasil skrining risiko tinggi dan tanda bahaya.",
            listOf(
                ArticleContentText(
                    text = "Hasil risiko tinggi perlu ditanggapi dengan serius. Jika ada pikiran menyakiti diri, bunuh diri, atau bahaya langsung, segera hubungi layanan darurat atau layanan krisis."
                ),
                ArticleContentText(
                    items = listOf(
                        "Jangan tinggal sendirian jika Anda merasa tidak aman.",
                        "Beri tahu orang tepercaya tentang apa yang sedang terjadi.",
                        "Hubungi psikiater, psikolog, dokter, layanan darurat, atau hotline krisis.",
                        "Jauhkan atau hindari akses ke benda yang dapat digunakan untuk menyakiti diri."
                    )
                )
            )
        ),
        "diet-and-depression-evidence" to ArticleText(
            "Makanan, Suasana Hati, dan Depresi: Apa Kata Bukti",
            "Ringkasan bukti tentang pola makan sehat sebagai dukungan tambahan untuk kesehatan mental.",
            listOf(
                ArticleContentText(
                    text = "Nutrisi tidak boleh menggantikan terapi, obat, atau perawatan klinis ketika hal tersebut dibutuhkan. Namun, pola makan sehat dapat mendukung kesehatan fisik dan dapat membantu gejala depresif sebagai bagian dari rencana perawatan yang lebih luas."
                ),
                ArticleContentText(
                    text = "Uji acak terkontrol SMILES mempelajari dukungan pola makan pada orang dewasa dengan depresi mayor dan menemukan perbaikan gejala yang lebih besar pada kelompok dukungan diet dibandingkan kelompok kontrol dukungan sosial. Bukti meta-analisis berikutnya juga menunjukkan bahwa perbaikan pola makan dapat mengurangi gejala depresi, meskipun metode penelitian berbeda-beda."
                ),
                ArticleContentText(
                    items = listOf(
                        "Prioritaskan sayuran, buah, kacang-kacangan, biji-bijian utuh, dan protein tanpa lemak.",
                        "Batasi makanan ultra-proses yang tinggi gula, garam, lemak jenuh, atau lemak trans.",
                        "Gunakan rekomendasi makanan sebagai dukungan kesehatan, bukan sebagai obat depresi.",
                        "Tanyakan kepada dokter atau ahli gizi terdaftar jika Anda memiliki kondisi medis, sedang hamil, gejala gangguan makan, alergi, atau interaksi obat."
                    )
                )
            )
        ),
        "sleep-rhythm-and-mood" to ArticleText(
            "Ritme Tidur dan Suasana Hati: Mengapa Rutinitas Penting",
            "Kebiasaan tidur yang teratur dapat membantu kestabilan energi, fokus, dan suasana hati.",
            listOf(
                ArticleContentText(
                    text = "Perubahan tidur umum terjadi saat suasana hati sedang rendah. Sebagian orang tidur jauh lebih lama dari biasanya, sementara yang lain sering terbangun, tidur terlalu larut, atau tetap lelah meski sudah beristirahat."
                ),
                ArticleContentText(
                    text = "Jadwal tidur dan bangun yang konsisten bukan obat depresi, tetapi dapat mendukung regulasi emosi, konsentrasi, nafsu makan, dan energi saat seseorang mencari bantuan yang sesuai."
                ),
                ArticleContentText(
                    items = listOf(
                        "Pilih waktu bangun yang realistis dan pertahankan secara mirip setiap hari.",
                        "Kurangi kecerahan layar dan hindari makan berat mendekati waktu tidur.",
                        "Gunakan rutinitas singkat sebelum tidur seperti latihan napas, membaca, atau doa dengan tenang.",
                        "Bicarakan dengan klinisi jika insomnia, tidur berlebihan, mimpi buruk, atau kelelahan siang hari terus berlanjut."
                    )
                )
            )
        ),
        "anxiety-vs-depression-overlap" to ArticleText(
            "Ketika Kecemasan dan Depresi Saling Tumpang Tindih",
            "Gejala cemas dan depresi dapat muncul bersama. Artikel ini membantu mengenali pola dan waktu mencari bantuan.",
            listOf(
                ArticleContentText(
                    text = "Kecemasan dan depresi dapat saling tumpang tindih. Seseorang mungkin merasa khawatir dan tegang, sekaligus merasa rendah, lelah, putus asa, atau kurang berminat pada aktivitas harian."
                ),
                ArticleContentText(
                    items = listOf(
                        "Perhatikan apakah kekhawatiran, kesedihan, mudah marah, perubahan tidur, atau perilaku menghindar mulai memengaruhi kehidupan harian.",
                        "Pantau gejala selama beberapa hari agar tenaga profesional dapat melihat polanya dengan lebih jelas.",
                        "Hindari mendiagnosis diri hanya dari satu hasil.",
                        "Cari bantuan profesional jika gejala menetap, memburuk, atau memengaruhi keamanan."
                    )
                )
            )
        ),
        "talking-to-family-about-results" to ArticleText(
            "Cara Berbicara dengan Keluarga Tentang Hasil Skrining",
            "Panduan memulai percakapan yang aman dan jelas dengan keluarga atau orang tepercaya.",
            listOf(
                ArticleContentText(
                    text = "Membicarakan kesehatan mental dapat terasa tidak nyaman, terutama jika keluarga belum terbiasa membahas emosi. Hasil skrining dapat digunakan sebagai titik awal percakapan, bukan sebagai label akhir."
                ),
                ArticleContentText(
                    items = listOf(
                        "Pilih seseorang yang kemungkinan besar dapat mendengarkan dengan tenang.",
                        "Mulailah dari apa yang Anda rasakan, bukan hanya dari skor.",
                        "Jelaskan jenis dukungan yang Anda butuhkan, seperti didengarkan, dibantu membuat janji perawatan, atau ditemani pada hari yang sulit.",
                        "Jika percakapan terasa tidak aman, hubungi konselor, klinisi, atau orang dewasa tepercaya lainnya."
                    )
                )
            )
        ),
        "when-to-see-a-psychiatrist" to ArticleText(
            "Kapan Sebaiknya Bertemu Psikiater?",
            "Tanda bahwa gejala mungkin membutuhkan penilaian dari psikiater atau tenaga kesehatan mental berlisensi.",
            listOf(
                ArticleContentText(
                    text = "Psikiater adalah dokter medis yang dapat menilai gejala kesehatan mental, mempertimbangkan faktor medis yang mungkin berperan, dan mendiskusikan pilihan perawatan termasuk rujukan terapi serta obat bila sesuai."
                ),
                ArticleContentText(
                    items = listOf(
                        "Pertimbangkan perawatan profesional jika gejala berlangsung lebih dari dua minggu atau mengganggu sekolah, pekerjaan, hubungan, atau perawatan diri.",
                        "Cari bantuan segera untuk pikiran menyakiti diri, pikiran bunuh diri, halusinasi, agitasi berat, atau ketidakmampuan menjaga diri tetap aman.",
                        "Bawa riwayat skrining, catatan gejala, daftar obat, dan pertanyaan Anda saat konsultasi.",
                        "Keputusan obat selalu harus dibuat bersama klinisi berlisensi."
                    )
                )
            )
        )
    )

    private val foodTranslations = mapOf(
        "oatmeal-banana" to FoodText("Oatmeal dengan Pisang", "Pilihan sarapan sederhana dari oat dan buah untuk membantu rutinitas pagi yang lebih stabil."),
        "salmon-brown-rice" to FoodText("Salmon dan Nasi Merah", "Ide makan seimbang dengan protein, lemak tidak jenuh, dan karbohidrat gandum utuh. Ikan lokal dapat menjadi pengganti."),
        "yogurt-berries" to FoodText("Yogurt dengan Beri", "Camilan dengan protein susu dan buah. Pilih yogurt tanpa gula tambahan bila memungkinkan."),
        "spinach-egg-bowl" to FoodText("Mangkuk Bayam dan Telur", "Menu praktis dengan telur dan sayuran hijau yang mudah disesuaikan dengan bahan lokal."),
        "tempe-vegetable-rice-bowl" to FoodText("Nasi Tempe dan Sayuran", "Menu terjangkau berbasis tempe, nasi, dan sayuran untuk dukungan energi harian."),
        "gado-gado-light-sauce" to FoodText("Gado-Gado dengan Saus Kacang Ringan", "Hidangan sayur khas Indonesia dengan saus kacang secukupnya agar tetap seimbang."),
        "chicken-soup-vegetables" to FoodText("Sup Ayam Sayuran", "Makanan hangat dengan protein dan sayuran yang cocok saat nafsu makan sedang turun."),
        "fruit-nut-snack" to FoodText("Camilan Buah dan Kacang", "Pilihan camilan sederhana yang menggabungkan buah dan lemak sehat dari kacang."),
        "tuna-sweet-potato-salad" to FoodText("Salad Tuna dan Ubi", "Kombinasi protein ikan dan karbohidrat kompleks yang praktis untuk makan utama."),
        "mung-bean-porridge" to FoodText("Bubur Kacang Hijau", "Makanan berbasis kacang-kacangan. Gunakan gula secukupnya sesuai kebutuhan kesehatan."),
        "avocado-wholegrain-toast" to FoodText("Roti Gandum Alpukat", "Sarapan cepat dengan lemak tidak jenuh dan karbohidrat gandum utuh."),
        "vegetable-omelet" to FoodText("Omelet Sayuran", "Menu telur dan sayuran yang mudah dibuat untuk sarapan atau makan ringan.")
    )

    private val specialistTranslations = mapOf(
        "5GvRbLGEeAsMNhLwOHv3" to SpecialistText(
            "Konsultan Psikiatri Anak dan Keluarga",
            "Dr. Tifa adalah psikiater yang berpengalaman mendampingi anak, remaja, dan keluarga dalam menghadapi masalah emosi, perilaku, serta adiksi. Pendekatannya menggabungkan asesmen psikiatri yang teliti, psikoedukasi, perencanaan perawatan kolaboratif, dan strategi koping yang praktis agar pasien serta keluarga memahami gejala dengan jelas. Ia berfokus membangun rasa aman, mengurangi stigma, dan membantu setiap pasien merasa didengar serta terlibat aktif dalam proses pemulihan."
        ),
        "6HwScMHFeByNkMxPqIw4" to SpecialistText(
            "Spesialis Trauma dan PTSD",
            "Dr. Cloud berfokus pada perawatan psikiatri berbasis trauma untuk pasien yang mengalami gejala PTSD, stres kronis, mati rasa emosional, dan kecemasan setelah pengalaman hidup yang berat. Ia menekankan keamanan, stabilisasi, dan pemulihan bertahap melalui asesmen terstruktur serta rencana perawatan berbasis bukti. Konsultasinya dirancang tenang dan praktis, dengan perhatian pada pengaruh trauma terhadap suasana hati, tidur, konsentrasi, relasi, dan fungsi harian."
        ),
        "7JxTdNIGfCzOlNyRjJx5" to SpecialistText(
            "Konselor Psikologi Klinis",
            "Dr. Aerith membantu individu menghadapi kecemasan, stres relasi, tekanan emosional, dan masa transisi hidup dengan empati serta struktur klinis. Gaya konselingnya memadukan asesmen psikologis, mendengarkan secara suportif, dan latihan keterampilan praktis agar klien lebih memahami pola pikiran, emosi, serta perilakunya. Ia menciptakan ruang terapi yang hangat untuk refleksi jujur, penguatan koping, komunikasi yang lebih sehat, dan rutinitas perawatan diri."
        ),
        "8KyUeOJHgDaPmOzSkKy6" to SpecialistText(
            "Spesialis Adiksi dan Pemulihan",
            "Dr. Barrett menyediakan perawatan kesehatan mental berfokus pemulihan bagi pasien yang menghadapi penggunaan zat, risiko kekambuhan, tekanan emosional, dan konflik sosial atau keluarga. Pendekatannya menekankan pemulihan menyeluruh, termasuk motivasi, keterampilan koping, sistem dukungan, tidur, manajemen stres, dan pencegahan kambuh jangka panjang. Ia bekerja bersama pasien untuk mengenali pemicu, membangun kembali rutinitas, dan mengubah tujuan perawatan menjadi langkah yang realistis."
        ),
        "9LzVfPKIhEbQnPzTlLz7" to SpecialistText(
            "Spesialis Gangguan Kognitif",
            "Dr. Red XIII berfokus pada keluhan kognitif dan neuropsikologis, termasuk perubahan memori, kesulitan perhatian, kebutuhan rehabilitasi kognitif, dan penyesuaian emosional terkait kondisi neurologis. Ia menggabungkan evaluasi cermat dengan rekomendasi praktis agar pasien dan keluarga memahami kekuatan, keterbatasan, serta strategi dukungan harian. Pendekatannya menekankan martabat pasien, komunikasi yang jelas, dan perencanaan berbasis bukti untuk fungsi yang lebih baik."
        ),
        "mentalys-psy-006" to SpecialistText(
            "Konsultan Psikiatri Dewasa",
            "Dr. Aditya berfokus pada perawatan psikiatri berbasis bukti untuk orang dewasa yang mengalami perubahan suasana hati, gejala kecemasan, stres kerja, dan kesulitan mempertahankan rutinitas harian. Ia melakukan asesmen psikiatri secara terstruktur, menjelaskan faktor yang mungkin berperan dengan bahasa yang mudah dipahami, serta membantu pasien menyusun rencana perawatan praktis. Rencana tersebut dapat mencakup rujukan terapi, penyesuaian gaya hidup, pemantauan gejala, dan diskusi pengobatan bila diperlukan."
        ),
        "mentalys-psy-007" to SpecialistText(
            "Konsultan Kesehatan Mental Remaja",
            "Dr. Nabila mendukung remaja dan dewasa muda yang menghadapi tekanan emosional, masalah harga diri, stres sekolah atau kampus, konflik keluarga, dan tanda awal gangguan suasana hati atau kecemasan. Ia menggunakan pendekatan yang peka terhadap perkembangan dan melibatkan keluarga secara tepat, sehingga pasien muda dapat mengekspresikan perasaannya dengan lebih aman. Konsultasinya berfokus pada keamanan, regulasi emosi, komunikasi, dan rutinitas realistis untuk membangun ketahanan diri."
        ),
        "mentalys-psy-008" to SpecialistText(
            "Konsultan Trauma dan Regulasi Emosi",
            "Dr. Bima menangani pasien dengan gejala terkait trauma, ketidakstabilan suasana hati, panik, mudah marah, dan respons stres berkepanjangan yang mengganggu kehidupan sehari-hari. Pendekatannya dimulai dari mendengarkan secara cermat dan memetakan gejala, lalu dilanjutkan dengan strategi stabilisasi, rencana perawatan, dan pilihan dukungan profesional yang sesuai kebutuhan pasien. Ia memperhatikan pola tidur, pemicu, penghindaran, regulasi emosi, dan rasa aman pasien sepanjang proses pemulihan."
        ),
        "mentalys-psy-009" to SpecialistText(
            "Konselor Terapi Perilaku Kognitif",
            "Dr. Laras memberikan konseling terstruktur untuk gejala depresi, kecemasan, regulasi emosi, motivasi yang menurun, dan kesulitan koping sehari-hari. Praktiknya menekankan strategi terapi perilaku kognitif, pengenalan pola pikir yang tidak membantu, perbaikan rutinitas, dan penguatan keterampilan memecahkan masalah secara realistis. Ia membantu klien mengubah pemahaman menjadi tindakan kecil yang konsisten, serta mendorong tindak lanjut profesional saat gejala menetap atau mengganggu."
        ),
        "mentalys-psy-010" to SpecialistText(
            "Konsultan Tidur dan Kecemasan",
            "Dr. Kevin membantu pasien memahami hubungan antara tidur, kecemasan, gejala panik, dan perubahan suasana hati melalui asesmen psikiatri yang praktis. Ia meninjau kebiasaan tidur, pola stres, gejala fisik, siklus pikiran, dan faktor gaya hidup yang mungkin mempertahankan distress. Konsultasinya dibuat jelas, suportif, dan berorientasi tindakan agar pasien dapat membangun strategi istirahat yang lebih baik serta regulasi emosi yang lebih stabil."
        )
    )

    private val specialistTitleId = mapOf(
        "MD, Psychiatry" to "dr., Sp.KJ",
        "Sp.KJ, Psychiatrist" to "Sp.KJ, Psikiater",
        "Clinical Psychologist" to "Psikolog Klinis",
        "PhD, Clinical Psychology" to "PhD, Psikologi Klinis"
    )

    private val specialistRoleId = mapOf(
        "Psychiatric Consultant" to "Konsultan Psikiatri",
        "Mental Health Specialist" to "Spesialis Kesehatan Mental",
        "Psychological Counselor" to "Konselor Psikologi",
        "Addiction Specialist" to "Spesialis Adiksi",
        "Cognitive Disorders Specialist" to "Spesialis Gangguan Kognitif"
    )

    private val specializationId = mapOf(
        "Depression" to "Depresi",
        "Anxiety Disorders" to "Gangguan Kecemasan",
        "Stress Management" to "Manajemen Stres",
        "Adolescent Psychiatry" to "Psikiatri Remaja",
        "School Stress" to "Stres Sekolah",
        "PTSD" to "PTSD",
        "Substance Abuse" to "Penyalahgunaan Zat",
        "Group Therapy" to "Terapi Kelompok",
        "Neurodegenerative Diseases" to "Penyakit Neurodegeneratif",
        "Cognitive Rehabilitation" to "Rehabilitasi Kognitif",
        "Child Psychiatry" to "Psikiatri Anak",
        "Addiction Therapy" to "Terapi Adiksi",
        "Trauma Therapy" to "Terapi Trauma",
        "PTSD Treatment" to "Penanganan PTSD",
        "Relationship Counseling" to "Konseling Relasi",
        "Anxiety Management" to "Manajemen Kecemasan",
        "Mood Disorders" to "Gangguan Suasana Hati",
        "Burnout Recovery" to "Pemulihan Burnout",
        "Depression Care" to "Perawatan Depresi",
        "Adolescent Mental Health" to "Kesehatan Mental Remaja",
        "Family Support" to "Dukungan Keluarga",
        "Self-Esteem Issues" to "Masalah Harga Diri",
        "Trauma Counseling" to "Konseling Trauma",
        "Stress Regulation" to "Regulasi Stres",
        "CBT" to "CBT",
        "Depression Counseling" to "Konseling Depresi",
        "Emotion Regulation" to "Regulasi Emosi",
        "Sleep Problems" to "Masalah Tidur",
        "Generalized Anxiety" to "Kecemasan Menyeluruh",
        "Panic Symptoms" to "Gejala Panik"
    )

    private val dayId = mapOf(
        "Monday" to "Senin",
        "Tuesday" to "Selasa",
        "Wednesday" to "Rabu",
        "Thursday" to "Kamis",
        "Friday" to "Jumat",
        "Saturday" to "Sabtu",
        "Sunday" to "Minggu"
    )
}
