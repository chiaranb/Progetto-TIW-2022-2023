package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;

import beans.User;
import dao.CategoryDAO;
import utils.ConnectionHandler;

@WebServlet("/CreateCategory")
@MultipartConfig
public class CreateCategory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	public CreateCategory() {
		super();
	}
	
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.connection = ConnectionHandler.getConnection(servletContext);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		// If the user is not logged in (not present in session) redirect to the login
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			String loginpath = getServletContext().getContextPath() + "/Login.html";
			response.sendRedirect(loginpath);
			return;
		}
		
		String name = request.getParameter("name");
		String fatherParam = request.getParameter("father");
		Integer fid = -1;
		
		try {
			name = StringEscapeUtils.escapeJava(name);
			fid = Integer.parseInt(fatherParam);
		} catch (NumberFormatException | NullPointerException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect parameter format");
			return;		
		}
		
		if(name == null || name.isEmpty() || name.isBlank()) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Missing parameters");
			return;		
		}
		
		User user = (User) session.getAttribute("user");
		CategoryDAO cDao = new CategoryDAO(connection);
		try {
			cDao.createCategory(name, fid, user.getId());
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Not possible to create category");
			return;
		}	
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
	}
	
	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e){
			e.printStackTrace();
		}
	}
}
