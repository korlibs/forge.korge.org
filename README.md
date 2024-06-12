# korge-forge-installer

## Windows 10 or greater

CMD:

```bash
curl -s -L https://raw.githubusercontent.com/korlibs/korge-forge-installer/6f1d80c947eeb02fc5ea2e08eae7aaa8f41922b0/install-korge-forge.cmd -o "%APPDATA%\install-korge-forge.cmd" && "%APPDATA%\install-korge-forge.cmd"
```

```bash
curl -s -L https://github.com/korlibs/korge-forge-installer/releases/download/v0.0.1/install-korge-forge.cmd -o "%APPDATA%\install-korge-forge.cmd" && "%APPDATA%\install-korge-forge.cmd"
```

```bash
powershell -NoProfile -ExecutionPolicy Bypass -Command "(New-Object Net.WebClient).DownloadFile('https://github.com/korlibs/korge-forge-installer/releases/download/v0.0.1/install-korge-forge.cmd', '%APPDATA:\=\\%\\install-korge-forge.cmd')" && "%APPDATA%\install-korge-forge.cmd"
```

## LINUX / MAC

```bash
sh -c "$(curl -fsSL https://raw.githubusercontent.com/korlibs/korge-forge-installer/a42b3bb1e888149b30efcfcd332f7f20a6c16975/install-korge-forge.sh)"
```

```bash
sh -c "$(curl -fsSL https://raw.githubusercontent.com/korlibs/korge-forge-installer/main/install-korge-forge.sh)"
```
