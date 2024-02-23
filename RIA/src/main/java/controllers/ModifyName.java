package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;

import dao.CategoryDAO;
import utils.ConnectionHandler;

@WebServlet("/ModifyName")
@MultipartConfig
public class ModifyName extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public ModifyName() {
		super();
	}

	public void init() throws ServletException {
		this.connection = ConnectionHandler.getConnection(getServletContext());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String name = request.getParameter("name");
		String fatherParam = request.getParameter("id");
		Integer id = -1;

		if (name == null || name.isEmpty() || name.isBlank()) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Missing parameters");
			return;
		}

		try {
			name = StringEscapeUtils.escapeJava(name);
			id = Integer.parseInt(fatherParam);
		} catch (NumberFormatException | NullPointerException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Incorrect parameter format");
			return;
		}

		CategoryDAO cDao = new CategoryDAO(connection);
		try {
			cDao.modifyName(name, id);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Not possible to modify name category");
			return;
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
