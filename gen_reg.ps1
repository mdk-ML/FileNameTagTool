# FileNameTagTool Registry Path Fixer
# Replaces hardcoded paths in addRightMenu.reg with actual paths

param(
    [string]$TemplateReg,
    [string]$OutputReg,
    [string]$JavawPath,
    [string]$JarPath,
    [string]$IcoPath
)

$gbk = [System.Text.Encoding]::GetEncoding('gb2312')
$content = [System.IO.File]::ReadAllText($TemplateReg, $gbk)

$oldJavaw = 'C:\\Program Files\\Java\\jdk-17\\bin\\javaw.exe'
$oldJar = 'C:\\Users\\MU\\Documents\\FileNameTagTool\\target\\filename-tagtool-1.0.0.jar'
$oldIco = 'C:\\Users\\MU\\Documents\\FileNameTagTool\\ico'

$newJavaw = $JavawPath.Replace('\', '\\')
$newJar = $JarPath.Replace('\', '\\')
$newIco = $IcoPath.Replace('\', '\\')

$content = $content.Replace($oldJavaw, $newJavaw)
$content = $content.Replace($oldJar, $newJar)
$content = $content.Replace($oldIco, $newIco)

[System.IO.File]::WriteAllText($OutputReg, $content, $gbk)
