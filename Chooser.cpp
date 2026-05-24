#include <iostream>
#include <string>
#include <cstdlib>

// ============================================================================
// 1. WINDOWS 平台原生實作
// ============================================================================
#if defined(_WIN32) || defined(_WIN64)
#include <windows.h>
#include <shobjidl.h>

void openFileDialog() {
    HRESULT hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
    if (SUCCEEDED(hr)) {
        IFileOpenDialog *pFileOpen;
        hr = CoCreateInstance(CLSID_FileOpenDialog, NULL, CLSCTX_ALL, 
            IID_IFileOpenDialog, reinterpret_cast<void**>(&pFileOpen));

        if (SUCCEEDED(hr)) {
            pFileOpen->SetTitle(L"選擇要空投發送的檔案");
            hr = pFileOpen->Show(NULL);

            if (SUCCEEDED(hr)) {
                IShellItem *pItem;
                hr = pFileOpen->GetResult(&pItem);
                if (SUCCEEDED(hr)) {
                    PWSTR pszFilePath;
                    hr = pItem->GetDisplayName(SIGDN_FILESYSPATH, &pszFilePath);
                    if (SUCCEEDED(hr)) {
                        // 以 UTF-8 輸出路徑供 Java 讀取
                        int size_needed = WideCharToMultiByte(CP_UTF8, 0, pszFilePath, -1, NULL, 0, NULL, NULL);
                        std::string strTo(size_needed, 0);
                        WideCharToMultiByte(CP_UTF8, 0, pszFilePath, -1, &strTo[0], size_needed, NULL, NULL);
                        std::cout << strTo.c_str() << std::endl;
                        CoTaskMemFree(pszFilePath);
                    }
                    pItem->Release();
                }
            }
            pFileOpen->Release();
        }
        CoUninitialize();
    }
}

// ============================================================================
// 2. macOS 平台原生實作 (利用 Objective-C++ 呼叫 Cocoa Framework)
// ============================================================================
#elif __APPLE__
#import <Cocoa/Cocoa.h>

void openFileDialog() {
    @autoreleasepool {
        NSOpenPanel* panel = [NSOpenPanel openPanel];
        [panel setTitle:@"選擇要空投發送的檔案"];
        [panel setCanChooseFiles:YES];
        [panel setCanChooseDirectories:NO];
        [panel setAllowsMultipleSelection:NO];

        // 強制讓對話框跑到最上層焦點
        [panel setLevel:CGShieldingWindowLevel() + 1];
        
        if ([panel runModal] == NSModalResponseOK) {
            NSURL* url = [[panel URLs] firstObject];
            std::cout << [[url path] UTF8String] << std::endl;
        }
    }
}

// ============================================================================
// 3. Linux 平台原生實作 (智慧偵測 GTK/Zenity 或 KDE/KDialog)
// ============================================================================
#else
#include <memory>
#include <stdexcept>
#include <array>

std::string execCommand(const char* cmd) {
    std::array<char, 128> buffer;
    std::string result;
    std::unique_ptr<FILE, decltype(&pclose)> pipe(popen(cmd, "r"), pclose);
    if (!pipe) return "";
    while (fgets(buffer.data(), buffer.size(), pipe.get()) != nullptr) {
        result += buffer.data();
    }
    return result;
}

void openFileDialog() {
    std::string path = "";
    // 優先嘗試 GNOME/Ubuntu 標配的 zenity
    path = execCommand("zenity --file-selection --title='選擇要空投發送的檔案' 2>/dev/null");
    
    // 如果沒有 zenity，嘗試 KDE 的 kdialog
    if (path.empty()) {
        path = execCommand("kdialog --getopenfilename . '所有檔案 (*)' --title '選擇要空投發送的檔案' 2>/dev/null");
    }
    
    if (!path.empty()) {
        // 去除換行符號
        path.erase(path.find_last_not_of("\n\r") + 1);
        std::cout << path << std::endl;
    }
}
#endif

// ============================================================================
// 主程式入口
// ============================================================================
int main() {
    openFileDialog();
    return 0;
}