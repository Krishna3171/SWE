$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

$cp = "backend/lib/junit-4.13.2.jar;backend/lib/hamcrest-core-1.3.jar"
$outDir = "backend/out"

New-Item -ItemType Directory -Force -Path $outDir | Out-Null

javac -cp $cp -d $outDir `
    backend/src/com/msa/model/*.java `
    backend/src/com/msa/dto/*.java `
    backend/src/com/msa/dao/*.java `
    backend/src/com/msa/db/*.java `
    backend/src/com/msa/service/*.java `
    backend/src/test/java/com/msa/*Test.java

java -cp "$outDir;$cp" org.junit.runner.JUnitCore `
    com.msa.AuthServiceTest `
    com.msa.MedicineServiceTest `
    com.msa.SalesServiceTest `
    com.msa.PurchaseServiceTest `
    com.msa.ReorderServiceTest `
    com.msa.ExpiredBatchDiscardServiceTest `
    com.msa.ProfitReportServiceTest
