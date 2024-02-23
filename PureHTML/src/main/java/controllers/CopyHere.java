package controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;

import beans.Category;
import dao.CategoryDAO;
import utils.ConnectionHandler;
import utils.TemplateHandler;

@WebServlet("/CopyHere")
public class CopyHere extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	public CopyHere() {
		super();
	}

	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		this.connection = ConnectionHandler.getConnection(servletContext);
		this.templateEngine = TemplateHandler.getEngine(servletContext, ".html");
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		// If the user is not logged in (not present in session) redirect to the login
		String loginpath = getServletContext().getContextPath() + "/Login.html";
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;
		}

		String idParam = request.getParameter("id");
		String fidParam = request.getParameter("fatherid");
		Integer fid = -1;
		Integer id = -1;

		if (idParam == null || fidParam == null) {
			forwardToErrorPage(request, response, "Invalid parameters for the copy here id is not inserted");
			return;
		}

		try {
			id = Integer.parseInt(idParam);
			fid = Integer.parseInt(fidParam);
		} catch (NumberFormatException e) {
			forwardToErrorPage(request, response, "Invalid parameters for the copy here id is not a number");
			return;
		}

		CategoryDAO cDao = new CategoryDAO(connection);
		List<Category> allCategories = null;
		List<Category> topCategories = null;
		try {
			cDao.copyHereSubtree(id, fid);
		} catch (Exception e) {
			forwardToErrorPage(request, response, e.getMessage());
			return;
		}

		try {
			allCategories = cDao.findAllCategories();
			topCategories = cDao.findCategoriesAndSubtrees();
		} catch (Exception e) {
			forwardToErrorPage(request, response, e.getMessage());
			return;
		}

		String path = "/WEB-INF/Home.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("allCategories", allCategories);
		ctx.setVariable("topCategories", topCategories);
		templateEngine.process(path, ctx, response.getWriter());
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
