<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"  uri="jakarta.tags.core"      %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Users · Trip-Planner</title>
    <style>
        body{font-family:sans-serif;margin:2rem;}
        table{border-collapse:collapse;width:100%}
        th,td{border:1px solid #ccc;padding:.4rem;text-align:left}
    </style>
</head>
<body>

<h1>Registered users (${fn:length(users)})</h1>
<table>
    <tr><th>ID</th><th>Registered</th></tr>
    <c:forEach var="u" items="${users}">
        <tr>
            <td>${u.chatId}</td>
            <td>${u.createdAt}</td>
        </tr>
    </c:forEach>
</table>

<p><a href="/admin">← back to admin</a></p>

</body>
</html>
