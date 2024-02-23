package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import beans.Category;

public class CategoryDAO {
	private Connection connection;

	public CategoryDAO(Connection connection) {
		this.connection = connection;
	}

	// Get all categories for the form category
	public List<Category> findAllCategories() throws SQLException {
		List<Category> categories = new ArrayList<>();

		connection.setAutoCommit(false);
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT id, name, position FROM catalog.category");) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category category = new Category();
					category.setId(result.getInt("id"));
					category.setName(result.getString("name"));
					category.setPosition(result.getInt("position"));
					categories.add(category);
					// Used to count the number of child
					category.setSubcategories(findDirectChild(category.getId()));
				}
			}
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(true);
		}

		Collections.sort(categories, Comparator.comparingInt(Category::getPosition));
		return categories;
	}

	// Find the direct child of category
	public List<Category> findDirectChild(int fid) throws SQLException {
		// Check if the father id exists
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT COUNT(*) FROM catalog.category WHERE id = ?");) {
			pstatement.setInt(1, fid);
			try (ResultSet resultSet = pstatement.executeQuery();) {
				if (resultSet.next() && resultSet.getInt(1) == 0) {
					throw new IllegalArgumentException("Invalid parameter id category does not exist");
				}
			}
		}

		List<Category> children = new ArrayList<>();
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT id, name, position FROM catalog.category WHERE father = ?");) {
			pstatement.setInt(1, fid);
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category category = new Category();
					category.setId(result.getInt("id"));
					category.setName(result.getString("name"));
					category.setPosition(result.getInt("position"));
					children.add(category);
				}
			}
		}
		return children;
	}

	// Create the tree structure
	public List<Category> findCategoriesAndSubtrees() throws SQLException {
		List<Category> tree = new ArrayList<>();
		connection.setAutoCommit(false);

		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT id FROM catalog.category WHERE father IS NULL");) {
			// Return the root category
			try (ResultSet result = pstatement.executeQuery();) {
				if (result.next()) {
					Category category = new Category();
					category.setId(result.getInt("id"));
					tree.add(category);
					findSubcategories(category);
				}
			}
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(true);
		}
		return tree;
	}

	// Find recursively the sub categories
	public void findSubcategories(Category category) throws SQLException {
		connection.setAutoCommit(false);

		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT id, name, position FROM catalog.category WHERE father = ?");) {
			pstatement.setInt(1, category.getId());
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					c.setPosition(result.getInt("position"));
					category.addSubcategories(c);
					findSubcategories(c);
				}
			}
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(true);
		}
	}

	// Create a new category
	public void createCategory(String name, int fid, int creator) throws SQLException {
		// Check if the father id exists
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT COUNT(*) FROM catalog.category WHERE id = ?");) {
			pstatement.setInt(1, fid);
			try (ResultSet resultSet = pstatement.executeQuery();) {
				if (resultSet.next() && resultSet.getInt(1) == 0) {
					throw new IllegalArgumentException("Invalid parameter for the form id category does not exist");
				}
			}
		}

		// Find the position of the last child of the father
		int position = 0;
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT MAX(position) FROM catalog.category WHERE father = ?");) {
			pstatement.setInt(1, fid);
			try (ResultSet resultSet = pstatement.executeQuery();) {
				if (resultSet.next()) {
					position = resultSet.getInt(1) + 1;
				} else {
					throw new IllegalArgumentException("Invalid parameter for the form id category does not exist");
				}
			}
		}

		// Insert the new category in the category table
		try (PreparedStatement pstatement = connection.prepareStatement(
				"INSERT into catalog.category (name, position, creator, father) VALUES(?, ?, ?, ?)");) {
			pstatement.setString(1, name);
			pstatement.setInt(2, position);
			pstatement.setInt(3, creator);
			pstatement.setInt(4, fid);
			pstatement.executeUpdate();
		}
	}

	// Copy the subtree
	public List<Category> copySubtree(int id) throws SQLException {
		List<Category> subtree = new ArrayList<>();

		String query = "SELECT id, name FROM catalog.category WHERE id = ?";

		connection.setAutoCommit(false);

		try (PreparedStatement pstatement = connection.prepareStatement(query);) {
			pstatement.setInt(1, id);
			try (ResultSet resultSet = pstatement.executeQuery();) {
				if (resultSet.next()) {
					Category category = new Category();
					category.setId(resultSet.getInt("id"));
					category.setName(resultSet.getString("name"));
					subtree.add(category);
					category.setSubcategories(findDirectChild(id));
					for (Category c : category.getSubcategories()) {
						subtree.addAll(copySubtree(c.getId()));
					}
				} else {
					throw new IllegalArgumentException("Invalid parameter for the copy id category does not exist");
				}
			}
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(true);
		}
		/*
		 * for (Category c : subtree) { System.out.println(c.getName()); }
		 */
		return subtree;
	}

	// Copy here a subtree
	public void copyHereSubtree(int id, int fid) throws SQLException {
		// Check the number of child of the father category
		try (PreparedStatement pstatementCount = connection
				.prepareStatement("SELECT COUNT(*) FROM catalog.category WHERE father = ?")) {
			pstatementCount.setInt(1, fid);
			try (ResultSet resultCount = pstatementCount.executeQuery()) {
				if (resultCount.next() && resultCount.getInt(1) == 9) {
					throw new IllegalArgumentException(
							"Not possibile insert new subcategory beacuse the category has reached the max number of subcategory");
				}
			}
		}

		connection.setAutoCommit(false);

		String query1 = "SELECT * FROM catalog.category WHERE id = ?";
		PreparedStatement pstatement1 = null;

		int position = 0;
		String query2 = "SELECT position FROM catalog.category WHERE id = ?";
		PreparedStatement pstatement2 = null;

		int idRoot = 0;
		String query3 = "INSERT into catalog.category (name, position, creator, father) VALUES (?, ?, ?, ?)";
		PreparedStatement pstatement3 = null;

		try {
			Category category = new Category();

			pstatement1 = connection.prepareStatement(query1);
			pstatement1.setInt(1, id);
			try (ResultSet resultSet = pstatement1.executeQuery();) {
				if (resultSet.next()) {
					category.setName(resultSet.getString("name"));
					category.setCreator(resultSet.getInt("creator"));
				} else {
					throw new IllegalArgumentException(
							"Invalid parameter for the copy here id category does not exist");
				}
			}

			// Find the number of children of the father
			int child = findDirectChild(fid).size();

			// Find the position of last child of the father
			pstatement2 = connection.prepareStatement(query2);
			pstatement2.setInt(1, fid);
			try (ResultSet resultSet = pstatement2.executeQuery();) {
				if (resultSet.next()) {
					position = resultSet.getInt(1);
				}
			}

			// Set the correct position
			if (child == 0) {
				category.setPosition(position * 10 + 1);
			} else {
				category.setPosition(child + position * 10 + 1);
			}

			// Insert the category in category table
			pstatement3 = connection.prepareStatement(query3, Statement.RETURN_GENERATED_KEYS);
			pstatement3.setString(1, category.getName());
			pstatement3.setInt(2, category.getPosition());
			pstatement3.setInt(3, category.getCreator());
			pstatement3.setInt(4, fid);
			pstatement3.executeUpdate();

			try (ResultSet resultSet = pstatement3.getGeneratedKeys()) {
				if (resultSet.next()) {
					idRoot = resultSet.getInt(1);
				}
			}

			// Insert child call recursively
			category.setSubcategories(findDirectChild(id));
			for (Category c : category.getSubcategories()) {
				copyHereSubtree(c.getId(), idRoot);
			}
		} catch (Exception e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(true);
		}
	}
}
