package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import beans.Category;
import dao.CategoryDAO;
import utils.ConnectionHandler;

@WebServlet("/Copy")
public class Copy extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public Copy() {
		super();
	}
	
	public void init() throws ServletException {
		connection = ConnectionHandler.getConnection(getServletContext());
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		
		String idParam = request.getParameter("categoryid");
		if(idParam == null || idParam.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Missing parameters");
			return;
		}
		
		Integer id = null;		
		try {
			id = Integer.parseInt(idParam);
		} catch (NumberFormatException | NullPointerException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Incorrect parameter format");
			return;		
		}
		
		CategoryDAO cDao = new CategoryDAO(connection);
		List<Category> subtree = null;

		try {
			subtree = cDao.copySubtree(id);
		} catch (Exception e) {
			e.printStackTrace();
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Not possible to recover copy");
			return;		
		}
		
		String json = new Gson().toJson(subtree);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(json);
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
