package com.hxh.apboa.common.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 描述：集合转树形结构工具类
 *
 * @author huxuehao
 **/
public class TreeUtils {

    /**
     * 将list转换成tree
     * @param sourceList 实现了TreeNode接口的元素集合
     * @return 将sourceList转换成树形结构的集合
     * @param <R> sourceList元素泛型
     * @param <T> TreeNode接口的泛型
     */
    public static <R extends TreeNode<T>, T> List<R> convertTree(List<R> sourceList) {
        return convertTree(sourceList, false);
    }

    /**
     * 将list转换成tree，克隆sourceList版
     * @param sourceList 实现了TreeNode接口的元素集合
     * @param cloneable 是否对sourceList进行克隆
     * @return 将sourceList转换成树形结构的集合
     * @param <R> sourceList元素泛型
     * @param <T> TreeNode接口的泛型
     */
    public static <R extends TreeNode<T>, T> List<R> convertTree(List<R> sourceList, boolean cloneable) {
        List<T> allIds = sourceList.stream().map(R::getId).collect(Collectors.toList());
        List<R> rootNodes = sourceList.stream().filter(item -> !allIds.contains(item.getParentId())).collect(Collectors.toList());

        if (cloneable) {
            List<R> sourceList_n = new LinkedList<>();
            for (R r : sourceList) {
                sourceList_n.add((R) r.clone());
            }
            rootNodes.forEach(item -> addChildren(item, sourceList_n));
        } else {
            rootNodes.forEach(item -> addChildren(item, sourceList));
        }

        return rootNodes;
    }

    /**
     * 根据父节点生成一个树
     * @param parent 父节点
     * @param sourceList 实现了TreeNode接口的元素集合
     */
    public static <R extends TreeNode<T>, T> R convertTreeOneNode(R parent, List<R> sourceList) {
        return convertTreeOneNode(parent, sourceList, false);
    }

    /**
     * 根据父节点生成一个树
     * @param parent 父节点
     * @param sourceList 实现了TreeNode接口的元素集合
     * @param cloneable 是否对sourceList进行克隆
     */
    public static <R extends TreeNode<T>, T> R convertTreeOneNode(R parent, List<R> sourceList, boolean cloneable) {
        List<R> rs = getJuniors(parent, sourceList, cloneable); // 获取parent下的所有节点
        R oneTree = ((R)parent.clone());
        oneTree.setChildren(convertTree(rs));
        return oneTree;
    }


    /**
     * 获取指定父节点下的所有节点
     * @param parent  指定父节点
     * @param sourceList 实现了TreeNode接口的元素集合
     * @return 指定父节点下的所有节点集合
     * @param <R> sourceList元素泛型
     * @param <T> TreeNode接口的泛型
     */
    public static <R extends TreeNode<T>, T> List<R> getJuniors(R parent, List<R> sourceList) {
        return getJuniors(parent, sourceList, false);
    }

    /**
     * 获取指定树节点下的所有节点，克隆sourceList版
     * @param parent  指定父节点
     * @param sourceList 实现了TreeNode接口的元素集合
     * @param cloneable 是否进行克隆sourceList。如果true则则会使用sourceList的克隆版进行后序操作；如果false则会直接使用sourceList，
     *                  会对sourceList的源数据造成影响
     * @return 指定父节点下的所有节点集合
     * @param <R> sourceList元素泛型
     * @param <T> TreeNode接口的泛型
     */
    public static <R extends TreeNode<T>, T> List<R> getJuniors(R parent, List<R> sourceList, boolean cloneable) {
        List<R> leaves = new LinkedList<>();
        if (cloneable) {
            List<R> sourceList_n = new LinkedList<>();
            for (R r : sourceList) {
                sourceList_n.add((R) r.clone());
            }
            fillLeaves(parent, sourceList_n, leaves);
        } else {
            fillLeaves(parent, sourceList, leaves);
        }

        return leaves;
    }

    /**
     * 获取指定父节点下的所有叶子节点
     * @param parent  指定父节点
     * @param sourceList 实现了TreeNode接口的元素集合
     * @return 指定父节点下的所有叶子节点集合
     * @param <R> sourceList元素泛型
     * @param <T> TreeNode接口的泛型
     */
    public static <R extends TreeNode<T>, T> List<R> getLeaves(R parent, List<R> sourceList) {
        return getLeaves(parent, sourceList, false);
    }

    /**
     * 获取指定树节点下的所有叶子节点，克隆sourceList版
     * @param parent  指定父节点
     * @param sourceList 实现了TreeNode接口的元素集合
     * @param cloneable 是否进行克隆sourceList。如果true则则会使用sourceList的克隆版进行后序操作；如果false则会直接使用sourceList，
     *                  会对sourceList的源数据造成影响
     * @return 指定父节点下的所有叶子节点集合
     * @param <R> sourceList元素泛型
     * @param <T> TreeNode接口的泛型
     */
    public static <R extends TreeNode<T>, T> List<R> getLeaves(R parent, List<R> sourceList, boolean cloneable) {
        List<R> leaves = new LinkedList<>();
        if (cloneable) {
            List<R> sourceList_n = new LinkedList<>();
            for (R r : sourceList) {
                sourceList_n.add((R) r.clone());
            }
            fillLeaves(parent, sourceList_n, leaves);
            filterLeaves(sourceList_n, leaves);
        } else {
            fillLeaves(parent, sourceList, leaves);
            filterLeaves(sourceList, leaves);
        }

        return leaves;
    }

    /**
     * 设置parent节点的Children
     * @param parent 指定父节点
     * @param sourceList 实现了TreeNode接口的元素集合
     * @param <R> sourceList元素泛型
     */
    private static <R extends TreeNode<?>> void addChildren(R parent, List<R> sourceList) {
        List<R> children = new LinkedList<>();
        /* 通过迭代器遍历，可以在遍历的过程中去除服务条件的元素 */
        fillChildren(parent, sourceList, children);
        if (children.isEmpty()) {
            return;
        }
        /* 设置Children */
        parent.setChildren(children);
        /* 递归设置Children */
        children.forEach(item -> addChildren(item, sourceList));
    }

    /**
     * 填充叶子节点, 会将指定父元素下面的所有节点填充到leaves中
     * @param parent 指定父节点
     * @param sourceList_ 实现了TreeNode接口的元素集合
     * @param leaves 存放叶子节点的集合容器
     * @param <R> sourceList元素泛型
     */
    private static <R extends TreeNode<T>, T> void fillLeaves(R parent, List<R> sourceList_, List<R> leaves) {
        List<R> children = new LinkedList<>();
        fillChildren(parent, sourceList_, children);
        if (children.isEmpty()) {
            return;
        }
        leaves.addAll(children);
        children.forEach(item -> fillLeaves(item, sourceList_, leaves));
    }

    /**
     * 通过迭代器遍历，可以在遍历的过程中去除符合条件的元素，
     * @param parent 指定父节点
     * @param sourceList_ 实现了TreeNode接口的元素集合，可以是克隆后的，也可以是源集合
     * @param children_ 存储子元素的集合
     * @param <R> sourceList元素泛型
     */
    private static <R extends TreeNode<?>> void fillChildren(R parent, List<R> sourceList_, List<R> children_) {
        /* 通过迭代器遍历，可以在遍历的过程中去除服务条件的元素 */
        for (Iterator<R> iterator = sourceList_.iterator(); iterator.hasNext();) {
            R node = iterator.next();
            /* 当前遍历到的元素属于parent的直接子元素 */
            if (Objects.equals(node.getParentId(), parent.getId())) {
                children_.add(node);
                /* 排除当前元素，提供性能，避免多次迭代 */
                iterator.remove();
            }
        }
    }

    /**
     * 过滤叶子节点，会将leaves中非叶子节点全部清除掉
     * @param sourceList_ 实现了TreeNode接口的元素集合
     * @param leaves 存放叶子节点的集合容器
     * @param <R> sourceList元素泛型
     * @param <T> TreeNode接口的泛型
     */
    private static <R extends TreeNode<T>, T> void filterLeaves(List<R> sourceList_, List<R> leaves) {
        /* 获取所有的父ID */
        List<T> parentIds = sourceList_.stream().map(R::getParentId).collect(Collectors.toList());
        /* 排除掉非叶子节点 */
        leaves.removeIf(node -> parentIds.contains(node.getId()));
    }


}

interface TreeNode<T> extends Cloneable {

    /* 设置ID */
    void setId(T id);

    /* 获取ID */
    T getId();

    /* 设置ID */
    void setParentId(T parentId);

    /* 获取ID */
    T getParentId();


    /* 设置节点的子节点列表 */
    void setChildren(List<? extends TreeNode<?>> children);

    /* 获取所有子节点 */
    List<? extends TreeNode<T>> getChildren();

    /* 拷贝, 自定义深拷贝还是浅深拷贝 **/
    Object clone();

}
