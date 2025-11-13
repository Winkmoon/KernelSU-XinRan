#!/bin/bash
# 批量替换 home_support_content 为可爱版本

echo "开始替换所有语言的 home_support_content..."

# 找到所有包含 home_support_content 的文件
files=$(grep -r '<string name="home_support_content">' . -l --include="strings.xml")

for file in $files; do
    echo "处理文件: $file"
    
    # 替换内容为可爱版本
    sed -i 's|<string name="home_support_content">[^<]*</string>|<string name="home_support_content">喵喵喵～瞧瞧欣然又在瞎搞什么～</string>|g' "$file"
    
    # 验证替换结果
    if grep -q '喵喵喵～瞧瞧欣然又在瞎搞什么～' "$file"; then
        echo "✅ $file - 替换成功"
    else
        echo "❌ $file - 替换失败"
    fi
done

echo "替换完成！"