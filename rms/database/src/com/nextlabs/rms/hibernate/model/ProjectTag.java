package com.nextlabs.rms.hibernate.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "project_tag", uniqueConstraints = {
    @UniqueConstraint(name = "u_tag_project_id", columnNames = { "tag_id", "project_id" }) })
@NamedQueries(value = {
    @NamedQuery(name = "deleteTagsByProject", query = "delete from ProjectTag where project.id = :projectId"),
    @NamedQuery(name = "deleteTags", query = "delete from ProjectTag where tag.id = :tagId") })
public class ProjectTag implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private Tag tag;
    private Project project;

    public ProjectTag() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "project_tag_id_seq")
    @SequenceGenerator(name = "project_tag_id_seq", sequenceName = "project_tag_id_seq")
    @Column(unique = true, nullable = false)
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ManyToOne
    @JoinColumn(name = "tag_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_tag_tag"))
    public Tag getTag() {
        return this.tag;
    }

    public void setTag(Tag tag) {
        this.tag = tag;
    }

    @ManyToOne
    @JoinColumn(name = "project_id", nullable = false, foreignKey = @ForeignKey(name = "fk_project_tag_project"))
    public Project getProject() {
        return this.project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

}
