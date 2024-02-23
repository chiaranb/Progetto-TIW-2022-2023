package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dao.CategoryDAO;
import utils.ConnectionHandler;

@WebServlet("/CopyHere")
public class CopyHere extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	
	public CopyHere() {
		super();
	}

	public void init() throws ServletException {
		this.connection = ConnectionHandler.getConnection(getServletContext());
	}
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
		throws ServletException, IOException {
		
		String idParam = request.getParameter("id");
		String fidParam = request.getParameter("fatherid");
		
		if(idParam == null || idParam.isEmpty() || fidParam == null || fidParam.isEmpty()) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Missing parameters");
			return;
		}
		
		Integer fid = null;
		Integer id = null;
		try {
			id = Integer.parseInt(idParam);
			fid = Integer.parseInt(fidParam);
		} catch (NumberFormatException | NullPointerException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Incorrect parameter format");
			return;
		}
		
		CategoryDAO cDao = new CategoryDAO(connection);
		try {
			cDao.copyHereSubtree(id, fid);
		} catch (Exception e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.getWriter().println("Not possible to recover copy here");
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
