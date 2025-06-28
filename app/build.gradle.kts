plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.1.0"
    id("kotlin-kapt")
}

android {
    namespace = "com.ai.papia"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ai.papia"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}
dependencies {
    // AndroidX Core, AppCompat, UI 라이브러리들을 최신 안정 버전으로 통일
    // 2025년 6월 현재 대략적인 최신 안정 버전을 기준으로 합니다.
    implementation("androidx.core:core-ktx:1.13.1")        // 1.12.0 -> 1.13.1 (가장 최신)
    implementation("androidx.appcompat:appcompat:1.7.0")    // 1.6.1 -> 1.7.0 (최신)
    implementation("com.google.android.material:material:1.12.0") // 1.11.0 -> 1.12.0 (최신)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // 이 버전은 괜찮습니다.
    implementation("androidx.activity:activity-ktx:1.9.0")  // 1.8.2 -> 1.9.0 (최신)
    implementation("androidx.fragment:fragment-ktx:1.7.0")  // 1.6.2 -> 1.7.0 (최신)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    // Material Calendar View (이 라이브러리가 문제의 원인일 가능성도 있습니다. 아래 3번 참고)
    implementation("com.prolificinteractive:material-calendarview:1.4.3") // 이 라이브러리는 업데이트가 오래되었습니다.

    // Room Database
    implementation("androidx.room:room-runtime:2.7.0-beta01") // 2.6.1 -> 2.7.0-beta01 (최신)
    implementation("androidx.room:room-ktx:2.7.0-beta01")
    implementation(libs.androidx.activity)     // 2.6.1 -> 2.7.0-beta01 (최신)
    kapt("androidx.room:room-compiler:2.7.0-beta01")        // 2.6.1 -> 2.7.0-beta01 (최신)

    // ViewModel and LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0") // 2.7.0 -> 2.8.0 (최신)
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")   // 2.7.0 -> 2.8.0 (최신)

    // SharedPreferences
    implementation("androidx.preference:preference-ktx:1.2.1") // 이 버전은 괜찮습니다.

    // Date picker - material 라이브러리를 이미 사용하므로 중복될 수 있습니다.
    // 보통 material 라이브러리에 date picker 위젯이 포함되어 있습니다.
    // 이 줄은 삭제하거나 위 material 라이브러리 의존성으로 통합하는 것이 좋습니다.
    // implementation("com.google.android.material:material:1.11.0") // 이미 위에서 1.12.0으로 선언됨

    // 테스트 라이브러리
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}