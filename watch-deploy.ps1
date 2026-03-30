# Monitor work_helper project for deployable packages
# When a new jar/zip is created in target directory, send to group and notify 云管家小王

$targetDir = "D:\JavaAI\work_helper\target"
$projectName = "work_helper"
$lastCheck = Get-Date
$stateFile = "$env:TEMP\work_helper_deploy_state.json"

Write-Host "[监控启动] 开始监控 $projectName 项目的部署包..."

# Load seen files from previous runs
$seenFiles = @{}
if (Test-Path $stateFile) {
    $seenFiles = @{}
}

# Track seen files to avoid duplicate notifications
$deployments = @()

while ($true) {
    Start-Sleep -Seconds 30
    
    try {
        # Look for jar, war, zip files
        $deployPackages = Get-ChildItem "$targetDir\*" -Include *.jar,*.war,*.zip -ErrorAction SilentlyContinue | 
            Where-Object { $_.LastWriteTime -gt $lastCheck }
        
        if ($deployPackages) {
            foreach ($pkg in $deployPackages) {
                $sizeMB = [math]::Round($pkg.Length / 1MB, 2)
                $timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
                
                # Output for OpenClaw to process
                Write-Host "DEPLOY_PACKAGE_FOUND|$($pkg.FullName)|$($pkg.Name)|$sizeMB|$timestamp"
                
                # Store in deployments list
                $deployments += @{
                    path = $pkg.FullName
                    name = $pkg.Name
                    size = $sizeMB
                    time = $timestamp
                }
            }
        }
        
        # Also check for any new jar files
        $allPackages = Get-ChildItem "$targetDir\*" -Include *.jar,*.war,*.zip -ErrorAction SilentlyContinue
        $allPackages | ForEach-Object { $seenFiles[$_.FullName] = $true }
        
        $lastCheck = Get-Date
    }
    catch {
        Write-Host "[警告] 检查失败: $_"
    }
}
