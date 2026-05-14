$root = "src\main\java";
$files = Get-ChildItem -Path $root -Recurse -Filter *.java
$importRemoved = 0
$fieldRemoved = 0
$filesChanged = 0

foreach ($file in $files) {
    $text = Get-Content -Raw -LiteralPath $file.FullName -Encoding UTF8
    $orig = $text

    # Remove unused imports
    $imports = [regex]::Matches($text, '^[ \t]*import[ \t]+(?!static)([\w\.]+);','MultiLine') | ForEach-Object { $_.Groups[1].Value }
    foreach ($imp in $imports) {
        if ($imp.EndsWith('.*')) { continue }
        $simple = $imp.Split('.')[-1]
        # Remove occurrences in import section only
        $textWithoutImports = $text -replace '^[ \t]*import[\s\S]*?;[ \t]*\r?\n','' -replace '^[ \t]*package[\s\S]*;\r?\n',''
        if (-not ([regex]::IsMatch($textWithoutImports, '\b' + [regex]::Escape($simple) + '\b'))) {
            # remove the import line
            $pattern = '^[ \t]*import[ \t]+' + [regex]::Escape($imp) + ';[ \t]*\r?\n?'
            $newText = [regex]::Replace($text, $pattern, '', 'MultiLine')
            if ($newText -ne $text) {
                $text = $newText
                $importRemoved++
            }
        }
    }

    # Remove unused private fields (simple heuristic)
    $fieldPattern = '^[ \t]*((?:@[^\r\n]+\r?\n)*)[ \t]*private[ \t]+(?:static[ \t]+)?[^;=\r\n]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;).*$'
    $lines = $text -split "\r?\n"
    $toRemove = @()
    for ($i=0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]
        $m = [regex]::Match($line, '^[ \t]*private[ \t]+(?:static[ \t]+)?[^;=\r\n]+\s+([A-Za-z_][A-Za-z0-9_]*)\s*(?:=|;)', 'IgnoreCase')
        if ($m.Success) {
            $name = $m.Groups[1].Value
            # count occurrences of the field name in the file excluding its own declaration
            $count = ([regex]::Matches($text, '\b' + [regex]::Escape($name) + '\b')).Count
            if ($count -le 1) {
                # remove the declaration line and preceding annotations if present
                $start = $i
                while ($start -gt 0 -and $lines[$start-1] -match '^[ \t]*@') { $start-- }
                for ($j=$start; $j -le $i; $j++) { $toRemove += $j }
                $fieldRemoved++
            }
        }
    }
    if ($toRemove.Count -gt 0) {
        $newLines = @()
        for ($i=0; $i -lt $lines.Length; $i++) {
            if ($toRemove -contains $i) { continue }
            $newLines += $lines[$i]
        }
        $text = ($newLines -join "`n") + "`n"
    }

    if ($text -ne $orig) {
        Set-Content -LiteralPath $file.FullName -Value $text -Encoding UTF8
        $filesChanged++
    }
}

Write-Output "Files changed: $filesChanged"
Write-Output "Imports removed: $importRemoved"
Write-Output "Private fields removed: $fieldRemoved"
