<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <title>数据分析报告</title>
    <style>
        table {
            width: 100%;
            border-collapse: collapse;
        }
        th, td {
            border: 1px solid black;
            padding: 8px;
            text-align: center;
        }
        th {
            background-color: #f2f2f2;
        }
        .summary {
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
<div>
    <h1>数据分析报告</h1>
    <#list items as item>
        ${item}
    </#list>
</div>
</body>
</html>