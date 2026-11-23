param([string[]]$Only)
$ErrorActionPreference = "Continue"
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8"
$jdk = "C:\Users\chen\DevTools\jdk-17.0.2"
$proj = "C:\Users\chen\DevTools\ToolboxPC"
# 测试数据一律落在临时目录：靠 TOOLBOX_DATA 让 Store 换根，绝不碰用户的 C:\Users\<你>\ToolboxPC
$dir = "$env:TEMP\toolbox-test-data"
$env:TOOLBOX_DATA = $dir
$bak = "$env:TEMP\toolbox-bak"
if (-not (Test-Path -LiteralPath $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
if (-not (Test-Path -LiteralPath "$bak\settings.txt")) {
    [IO.File]::WriteAllLines("$bak\settings.txt", [string[]]@("sound=0","strong=0","pin=1","navOrder=schedule,calc,ledger,memo,counter,pomodoro,countdown,stats,settings"), (New-Object Text.UTF8Encoding($false)))
}
if (-not (Test-Path -LiteralPath "$bak\schedule.txt")) {
    [IO.File]::WriteAllLines("$bak\schedule.txt", [string[]]@(), (New-Object Text.UTF8Encoding($false)))
}

# 数据日期跟着系统当天走，避免隔天跑回归时"今天"错位
$today = (Get-Date).Date
$dow = [int]$today.DayOfWeek
$monday = if ($dow -eq 0) { $today.AddDays(-6) } else { $today.AddDays(1 - $dow) }

$t0   = $today.ToString("yyyy-MM-dd")            # 今天
$t1   = $today.AddDays(1).ToString("yyyy-MM-dd")  # 明天
$t_1  = $today.AddDays(-1).ToString("yyyy-MM-dd") # 昨天
$t_4  = $today.AddDays(-4).ToString("yyyy-MM-dd")
$t_6  = $today.AddDays(-6).ToString("yyyy-MM-dd")
$t_9  = $today.AddDays(-9).ToString("yyyy-MM-dd")
$t_10 = $today.AddDays(-10).ToString("yyyy-MM-dd")
$m1   = $monday.ToString("yyyy-MM-dd")            # 本周一
$m8   = $monday.AddDays(-6).ToString("yyyy-MM-dd")   # 上周二
$m9   = $monday.AddDays(-5).ToString("yyyy-MM-dd")   # 上周三
$m15  = $monday.AddDays(-12).ToString("yyyy-MM-dd")  # 上上周三
$a20  = $today.AddDays(-20).ToString("yyyy-MM-dd")
$a40  = $today.AddDays(-40).ToString("yyyy-MM-dd")

$fifteenth = Get-Date -Year $today.Year -Month $today.Month -Day 15 -Hour 0 -Minute 0 -Second 0
if ($fifteenth -lt $today) { $fifteenth = $fifteenth.AddMonths(1) }
$f25 = Get-Date -Year $today.Year -Month $today.Month -Day 25 -Hour 0 -Minute 0 -Second 0
if ($f25 -lt $today) { $f25 = $f25.AddMonths(1) }
$f15s = $fifteenth.ToString("yyyy-MM-dd")
$f25s = $f25.ToString("yyyy-MM-dd")

$TODO = @(
"0`t今天的事`t$t0","0`t九天前没动`t$t_9","0`t四天前`t$t_4",
"0`t昨天没做`t$t_1","0`t六天前`t$t_6","0`t十天前`t$t_10",
"0`t明天的事`t$t1","0`t十五号`t$f15s","0`t二十五号`t$f25s",
"0`t没定日子的`t-",
"1`t今天已做完`t$t0","1`t今天第二件`t$t0",
"1`t周一做的`t$m1","1`t上周做的`t$m9","1`t上周第二条`t$m8",
"1`t上上周做的`t$m15",
"1`t二十天前（已归档）`t$a20","1`t四十天前（已归档）`t$a40")

function ResetBox($withDisabled) {
    Get-Process java,小工具箱PC -ErrorAction SilentlyContinue | Stop-Process -Force
    $waited = 0
    while ((Get-Process java,小工具箱PC -ErrorAction SilentlyContinue) -and $waited -lt 20) {
        Start-Sleep -Milliseconds 500
        Get-Process java,小工具箱PC -ErrorAction SilentlyContinue | Stop-Process -Force
        $waited++
    }
    Start-Sleep -Seconds 4
    Get-ChildItem -LiteralPath $dir -Force -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -ne "settings.txt" -and $_.Name -ne "schedule.txt" } |
        ForEach-Object { Remove-Item -LiteralPath $_.FullName -Recurse -Force -ErrorAction SilentlyContinue }
    if ($withDisabled) {
        $s = @("sound=0","strong=0","pin=1","navOrder=schedule,calc,ledger,memo,counter,pomodoro,countdown,stats,settings","disabled=calc,memo,counter")
        [IO.File]::WriteAllLines("$dir\settings.txt", [string[]]$s, (New-Object Text.UTF8Encoding($false)))
    } else {
        Copy-Item -LiteralPath "$bak\settings.txt" -Destination "$dir\settings.txt" -Force
    }
    Copy-Item -LiteralPath "$bak\schedule.txt" -Destination "$dir\schedule.txt" -Force
    [IO.File]::WriteAllLines("$dir\todo.txt", [string[]]$TODO, (New-Object Text.UTF8Encoding($false)))
    return (Get-Content -LiteralPath "$dir\todo.txt" -Encoding UTF8).Count
}

$tests = @("FeatureTest46","FeatureTest47","FeatureTest48","FeatureTest49","FeatureTest50","FeatureTest51","FeatureTest52","FeatureTest53","FeatureTest54","FeatureTest55","FeatureTest56","FeatureTest57","FeatureTest58","FeatureTest59","FeatureTest60","FeatureTest61","FeatureTest62","FeatureTest63","FeatureTest64","FeatureTest65","FeatureTest66","FeatureTest67","FeatureTest68","FeatureTest69","FeatureTest70","FeatureTest71","FeatureTest72","FeatureTest73","FeatureTest74","FeatureTest75","FeatureTest76","FeatureTest77","FeatureTest78")
if ($Only) { $tests = @($tests | Where-Object { $Only -contains $_ }) }

# 记下桌面原有的 ui-*.png，跑完只删本轮新生成的验收图，用户自己的图一张不碰
$desk = "C:\Users\chen\Desktop"
$shotsBefore = @(Get-ChildItem -LiteralPath $desk -Filter "ui-*.png" -ErrorAction SilentlyContinue | ForEach-Object { $_.Name })

foreach ($t in $tests) {
    $withDisabled = ($t -eq "FeatureTest59")
    $n = ResetBox $withDisabled
    Write-Output ("SEED " + $t + " todo=" + $n)
    if ($n -ne 18) { Write-Output ("SEED-BAD " + $t + " todo=" + $n) }

    $log = "$env:TEMP\$t.log"
    $err = "$env:TEMP\$t.err"
    $args = @("-cp", "$proj\shot;$proj\build\classes;$proj\lib\flatlaf.jar", $t)
    if ($t -eq "FeatureTest60") {
        # 护眼逻辑用 headless 跑，避免真的弹休息窗口打断桌面
        $args = @("-Djava.awt.headless=true") + $args
    }
    $p = Start-Process -FilePath "$jdk\bin\java.exe" -ArgumentList $args `
        -RedirectStandardOutput $log -RedirectStandardError $err -WindowStyle Hidden -PassThru
    if (-not $p.WaitForExit(200000)) {
        try { $p.Kill() } catch {}
        Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force
        Write-Output ("RESULT " + $t + " :: TIMEOUT(>200s)")
        continue
    }
    $lines = @()
    if (Test-Path -LiteralPath $log) { $lines = @(Get-Content -LiteralPath $log -Encoding UTF8) }
    $tail = ($lines | Where-Object { $_ -match 'ALL PASS|SOME FAIL|SOME FAILED|Exception' } | Select-Object -Last 2) -join " / "
    if (-not $tail -and (Test-Path -LiteralPath $err)) {
        $et = (Get-Content -LiteralPath $err -Encoding UTF8 | Where-Object { $_ -match 'Exception|Error' } | Select-Object -Last 1)
        if ($et) { $tail = "ERR: " + $et }
    }
    Write-Output ("RESULT " + $t + " :: " + $tail)
}

Get-Process java,小工具箱PC -ErrorAction SilentlyContinue | Stop-Process -Force
Get-ChildItem -LiteralPath $desk -Filter "ui-*.png" -ErrorAction SilentlyContinue |
    Where-Object { $shotsBefore -notcontains $_.Name } |
    ForEach-Object { Remove-Item -LiteralPath $_.FullName -Force -ErrorAction SilentlyContinue }
Write-Output "REGRESSION-DONE"
