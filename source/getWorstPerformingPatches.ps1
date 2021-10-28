Get-Content "C:/path-to-list-of-worst-performing-patches.txt"
| ForEach {Move-Item -Path .\$_ -Destination "C:/path-to-directory-of-removed-patches"
}