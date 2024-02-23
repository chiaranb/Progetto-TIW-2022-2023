package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import beans.User;
import dao.CategoryDAO;
import utils.ConnectionHandler;
import utils.TemplateHandler;

@WebServlet("/CreateCategory")
public class CreateCategory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public CreateCategory() {
		super();
	}

	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.connection = ConnectionHandler.getConnection(servletContext);
		this.templateEngine = TemplateHandler.getEngine(servletContext, ".html");
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
			forwardToErrorPage(request, response, "Invalid form parameters for the creation of new category");
			return;
		}

		if (name == null || name.isEmpty() || name.isBlank()) {
			forwardToErrorPage(request, response, "Name of category is not valid");
			return;
		}

		User user = (User) session.getAttribute("user");
		CategoryDAO cDao = new CategoryDAO(connection);
		try {
			cDao.createCategory(name, fid, user.getId());
		} catch (Exception e) {
			forwardToErrorPage(request, response, e.getMessage());
			return;
		}

		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/Home";
		response.sendRedirect(path);
	}

	private void forwardToErrorPage(HttpServletRequest request, HttpServletResponse response, String error)
			throws ServletException, IOException {

		request.setAttribute("error", error);
		forward(request, response, "/WEB-INF/error.html");
		return;
	}

	private void forward(HttpServletRequest request, HttpServletResponse response, String path)
			throws ServletException, IOException {

		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		templateEngine.process(path, ctx, response.getWriter());
	}

	@Override
	public void destroy() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
