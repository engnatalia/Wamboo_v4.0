# 🌱 Wamboo (Open Source)

Eco-friendly **video compressor** 📸🎥  
Designed to help users **save storage, reduce power usage, and lower their environmental footprint** 🌍

Now released as **open source** so the community can improve and keep it alive 🚀

---

## ⚠️ Why Open Source?

I had to unpublish Wamboo Eco-compressor from Google Play because of new requirements from Google and dependencies:

1. **16 KB memory page support**
   - All apps targeting Android 15+ must support 16 KB page sizes.
   - My last production build did not.

2. **Target API 35 (Android 15)**
   - Mandatory starting **Nov 1, 2025**.
   - Apps not targeting Android 15 or higher cannot be updated.

3. **FFmpeg-Kit discontinued**
   - The library I used for video processing was retired due to **legal risks** and **maintenance costs**.
   - [More info & solutions here](https://proandroiddev.com/ffmpeg-kit-16-kb-page-size-in-android-d522adc5efa2#da91).

Because of all this, and since I had neither the **time to do it myself** nor the **resources to pay developers**, I couldn’t keep updating the app.  
👉 Instead of letting it die, I’m sharing the source so developers can take over 💚

---

## ✨ Features (Current)

- Simple **eco video compressor & camera** 📸🎥
- Helps save storage & reduce power consumption 🌱
- Option to **save space without losing video quality** 💾✨

---

## 💡 Ideas for Future Development

If you want to contribute, here are directions I envisioned:
- Replace **FFmpeg-kit with alternatives**
- Update to **target API 35 (Android 15)**
- Ensure support for **16 KB memory pages** 🛠️
- Improve UI & design ✨
- Add new compression options 🔧
- Support new eco-compression methods ⚡
- Add new metrics for **measuring power consumption and associated pollution** ⚡🌍
- Or simply… add **any improvements you imagine** 🚀

---

## 🛠️ How You Can Help


To contribute to this project, follow these steps:

1. **Fork the Repository**: Click the "Fork" button at the top right of the Wamboo project repository to create a copy of the repository in your GitHub account.

2. **Clone the Repository**: In your terminal, run the following command to clone your forked repository to your local machine:

   ```bash
   git clone https://github.com/engnatalia/Wamboo_v4.git
   ```

3. **Create a New Branch**: Create a new branch for your contribution using the following command:

   ```bash
   git checkout -b your-branch-name
   ```

4. **Make Changes**: Make your desired changes to the codebase.

5. **Commit Changes**: Commit your changes with a descriptive commit message:

   ```bash
   git commit -m "Add new feature"
   ```

6. **Push Changes**: Push your changes to your forked repository on GitHub:

   ```bash
   git push origin your-branch-name
   ```

7. **Open a Pull Request**: Go to the Wamboo project repository and click the "New Pull Request" button. Describe your changes and submit the pull request.

8. **Review and Collaborate**: Your pull request will be reviewed by project maintainers. Be open to feedback and collaborate as needed.

By following these guidelines, you can contribute to our project and help make Harmony Valley even better. We appreciate your contributions!

Additionally, we have two Matlab developments for VP9 and HEVC motion estimation available at [WambooVP9ME_v1.0](https://github.com/engnatalia/WambooVP9ME_v1.0/blob/main/README.md) and [WambooHEVCME_v1.0](https://github.com/engnatalia/WambooHEVCME_v1.0/tree/main), where your contributions are welcome!

## License

This project is licensed under the Apache License 2.0. By contributing to this project, you agree to abide by the terms and conditions of this license. You can find more details in the [LICENSE](LICENSE.md) file.

## Copyright and Intellectual Property

This project is copyrighted by Natalia Molinero Mingorance. The original code and its contributions are protected under copyright laws. Natalia Molinero Mingorance has also registered this project with the Intellectual Property Office in Spain.

For more information about the Harmony Valley project and its goals, visit the [Harmony Valley project page](https://linktr.ee/wamboo.harmonyvalley).

Please replace "your-username" with your actual GitHub username and "your-branch-name" with a descriptive branch name for your changes. Make sure to adjust the URLs and project details to match the actual Harmony Valley project.

Thank you for your interest in contributing to Wamboo!
```
