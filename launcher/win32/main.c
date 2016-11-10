/*  Copyright 2010,2016 cygx <cygx@cpan.org>
    Distributed under the Boost Software License, Version 1.0
*/

#include <windows.h>
#include <wchar.h>

#define FORMAT L"java.exe -jar \"%.*ls\\crow.jar\" %ls"

static const wchar_t *parse(wchar_t *op, const wchar_t *ip,
        const wchar_t **mark, const wchar_t **end) {
    *mark = op;

    for(_Bool quote = 0; *ip; ++ip) {
        switch(*ip) {
            case L' ':
            if(quote) break;
            ++ip;
            goto FINISH;

            case L'"':
            quote = !quote;
            continue;

            case L'/':
            case L'\\':
            *mark = op;
            *op++ = L'\\';
            continue;
        }

        *op++ = *ip;
    }

FINISH:
    *op = 0;
    *end = op;
    return ip;
}

int main(void) {
    const wchar_t *cmdline = GetCommandLineW();
    size_t len = wcslen(cmdline);

    wchar_t buffer[len + 1];
    const wchar_t *path_end, *end;
    const wchar_t *args = parse(buffer, cmdline, &path_end, &end);

    int path_len = (int)(path_end - buffer);

    wchar_t cmdline_buffer[sizeof FORMAT / sizeof *FORMAT + len];
    _snwprintf(cmdline_buffer, sizeof cmdline_buffer, FORMAT,
        path_len ? path_len : 1, path_len ? buffer : L".", args);

    STARTUPINFOW startup;
    ZeroMemory(&startup, sizeof startup);
    startup.cb = sizeof startup;

    PROCESS_INFORMATION proc;
    ZeroMemory(&proc, sizeof proc);

    if(!CreateProcessW(NULL, cmdline_buffer, NULL, NULL, FALSE, 0, NULL, NULL,
        &startup, &proc)) return (int)GetLastError();

    WaitForSingleObject(proc.hProcess, INFINITE);
    CloseHandle(proc.hProcess);
    CloseHandle(proc.hThread);

    return 0;
}
