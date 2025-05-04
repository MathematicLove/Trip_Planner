<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c"   uri="jakarta.tags.core"      %>
<%@ taglib prefix="fn"  uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <title>Admin · Trip-Planner</title>
    <style>
        body{font-family:sans-serif;margin:2rem;}
        table{border-collapse:collapse;width:100%}
        th,td{border:1px solid #ccc;padding:.4rem;text-align:left}
        input[type=text]{width:100%}
    </style>
</head>
<body>

<h1>Admin panel</h1>

<h2>Broadcast</h2>
<form method="post" action="/admin/broadcast">
    <input type="text" name="msg" placeholder="message…"/>
    <button type="submit">Send</button>
</form>
<c:if test="${not empty delivered}">
    <p>sent to <strong>${delivered}</strong> users</p>
</c:if>

<hr/>

<h2>Users (${fn:length(users)})</h2>
<table>
    <tr><th>ID</th><th>Registered</th></tr>
    <c:forEach var="u" items="${users}">
        <tr>
            <td>${u.chatId}</td>
            <td>${u.createdAt}</td>
        </tr>
    </c:forEach>
</table>

</body>
</html>
