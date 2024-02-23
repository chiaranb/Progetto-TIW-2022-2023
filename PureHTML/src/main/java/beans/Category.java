package beans;

import java.util.ArrayList;
import java.util.List;

public class Category {
	
	private int id;
	private String name;
	private int position;
	private int creator;
	private List<Category> subCategories = new ArrayList<>();
	
	public int getId() {
		return this.id;
	}
	
	public void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public int getPosition() {
		return this.position;
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	
	public int getCreator() {
		return this.creator;
	}
	
	public void setCreator(int creator) {
		this.creator = creator;
	}
	
	public List<Category> getSubcategories() {
		return subCategories;
	}
	
	public void setSubcategories(List<Category> subCategories) {
		this.subCategories = subCategories;
	}

	public void addSubcategories(Category subCategory) {
		subCategories.add(subCategory);
	}
	
	@Override
	public boolean equals(Object obj) {
	    if (obj == null) {
	        return false;
	    }
	    if (!(obj instanceof Category)) {
	        return false;
	    }
	    Category other = (Category) obj;
	    if (this.id != other.id) {
	        return false;
	    }
	    return true;
	}
}
