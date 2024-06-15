// docker run --rm -v ".":/usr/src/myapp -w /usr/src/myapp bensuperpc/tinycc tcc korge-forge-installer.c
// del korge-forge-installer.exe && c:\dev\tcc\tcc korge-forge-installer.c icons.res && korge-forge-installer
// pacman -Syu
// pacman -S mingw-w64-x86_64-binutils
// pacman -S mingw-w64-x86_64-gcc
// windres icons.rc -O coff -o icons.res
// icons.rc: IDI_ICON1 ICON "install.ico"

#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>    // bool type
#include "sha1.c"
#include <sys/stat.h>

// return true if the file specified by the filename exists
int file_exists(const char *filename) {
    FILE *fp = fopen(filename, "r");
    if (fp != NULL) fclose(fp);
    return fp != NULL;
}

bool dir_exists(const char *filename) {
    struct stat sb;
    return stat(filename, &sb) == 0 && S_ISDIR(sb.st_mode);
}

const char *HEX = "0123456789abcdef";

int hex(unsigned char *bin, char *hex, int len) {
    for (int n = 0; n < len; n++) {
        hex[n * 2 + 0] = HEX[((bin[n] >> 4) & 0xF)];
        hex[n * 2 + 1] = HEX[((bin[n] >> 0) & 0xF)];
    }
    hex[len * 2 + 1] = 0;
    return 0;
}

int sha1_memory(char digest[20], char *data, int size) {
    //SHA1(digest, data, size);
    SHA1_CTX ctx;
    SHA1Init(&ctx);
    SHA1Update(&ctx, data, size);
    SHA1Final(digest, &ctx);
    return 0;
}

int sha1_file(char digest[20], const char *filename) {
    FILE *fp = fopen(filename, "rb");
    if (fp == NULL) { return -1; }
    fseek(fp, 0L, SEEK_END);
    long long int file_size = ftell(fp);
    fseek(fp, 0L, SEEK_SET);

    char *mem = malloc(file_size);
    fread(mem, file_size, 1, fp);
    sha1_memory(digest, mem, file_size);
    free(mem);
    fclose(fp);

}

int downloadFileIfNotExists(char *url, char *file, char *sha1) {
    char temp[4096] = {0};
    char fileTmp[4096] = {0};
    char digest[20] = {0};
    char digest_hex[41] = {0};

    sprintf(fileTmp, "%s.tmp", file);

    if (!file_exists(file)) {
        printf("Downloading %s into %s...\n", url, fileTmp);
        sprintf(temp, "curl -sL \"%s\" -o \"%s\"", url, fileTmp);
        //sprintf("curl -sL %s -o %s", url, file);
        system(temp);

        sha1_file(digest, fileTmp);
        hex(digest, digest_hex, 20);
        if (strcmp(digest_hex, sha1) != 0) {
            printf("HASH ERROR %s != %s!\n", digest_hex, sha1);
            exit(-1);
        }

        if (rename(fileTmp, file) == 0) {
            printf("File renamed successfully.\n");
        } else {
            printf("Error renaming file %s, %s\n", fileTmp, file);
            perror("Error renaming file");
            return 1;
        }
        //printf("SHA1: %s\n", digest_hex);
    } else {
        printf("Already downloaded %s...\n", url);
    }
}

int main(int argc, const char *argv[]) {
    char argsAllp[4096] = {0};
    for (int n = 1; n < argc; n++) {
        strcat(argsAllp, "\"");
        strcat(argsAllp, argv[n]); // @TODO: This is not escaping the argument
        strcat(argsAllp, "\"");
        strcat(argsAllp, " ");
    }
    //printf("DONE! : `%s`\n", temp);

    system("mkdir korge-forge-installer 2> NUL");
    downloadFileIfNotExists("https://github.com/korlibs/forge.korge.org/releases/download/v0.1.1/korge-forge-installer.jar", "korge-forge-installer\\korge-forge-installer.jar", "8b29794bbc14e50f7c4d9c4b673aabbbdcf6cfd1");
    downloadFileIfNotExists("https://github.com/korlibs/universal-jre/releases/download/0.0.1/OpenJDK21U-jre_x64_windows_hotspot_21.0.3_9.zip", "korge-forge-installer\\korge-forge-installer-jre.zip", "0a36d67c443387bab59e335b8a073d7d0f3a9575");
    if (!file_exists("korge-forge-installer/jdk-21.0.3+9-jre/bin/java.exe")) {
        printf("Unzipping JRE...\n");
        system("tar -xzf korge-forge-installer\\korge-forge-installer-jre.zip -C korge-forge-installer");
    }

    char temp[4096] = {0};
    sprintf(temp, "cd korge-forge-installer && jdk-21.0.3+9-jre\\bin\\java.exe -jar korge-forge-installer.jar %s", argsAllp);
    system(temp);
    //system("curl -s -L \"https://forge.korge.org/install-korge-forge.cmd\" -o \"install-korge-forge.cmd\"");
    //system("install-korge-forge.cmd");
    return 0;
}
