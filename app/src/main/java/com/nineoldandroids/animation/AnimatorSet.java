/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nineoldandroids.animation;

import java.util.ArrayList;
import java.util.HashMap;

import android.view.animation.Interpolator;

/**
 * This class plays a set of {@link Animator} objects in the specified order. Animations
 * can be set up to play together, in sequence, or after a specified delay.
 * 
 * <p>
 * There are two different approaches to adding animations to a <code>AnimatorSet</code>: either the {@link AnimatorSet#playTogether(Animator[]) playTogether()} or
 * {@link AnimatorSet#playSequentially(Animator[]) playSequentially()} methods can be called to add a set of animations all at once, or the {@link AnimatorSet#play(Animator)} can be used in conjunction with
 * methods in the {@link AnimatorSet.Builder Builder} class to add animations one by one.
 * </p>
 * 
 * <p>
 * It is possible to set up a <code>AnimatorSet</code> with circular dependencies between its animations. For example, an animation a1 could be set up to start before animation a2, a2 before a3, and a3 before
 * a1. The results of this configuration are undefined, but will typically result in none of the affected animations being played. Because of this (and because circular dependencies do not make logical sense
 * anyway), circular dependencies should be avoided, and the dependency flow of animations should only be in one direction.
 */
public final class AnimatorSet extends Animator {

	/**
	 * Internal variables
	 * NOTE: This object implements the clone() method, making a deep copy of any referenced
	 * objects. As other non-trivial fields are added to this class, make sure to add logic
	 * to clone() to make deep copies of them.
	 */

	/**
	 * Tracks animations currently being played, so that we know what to
	 * cancel or end when cancel() or end() is called on this AnimatorSet
	 */
	private ArrayList<Animator> mPlayingSet = new ArrayList<Animator>();

	/**
	 * Contains all nodes, mapped to their respective Animators. When new
	 * dependency information is added for an Animator, we want to add it
	 * to a single node representing that Animator, not create a new Node
	 * if one already exists.
	 */
	private HashMap<Animator, Node> mNodeMap = new HashMap<Animator, Node>();

	/**
	 * Set of all nodes created for this AnimatorSet. This list is used upon
	 * starting the set, and the nodes are placed in sorted order into the
	 * sortedNodes collection.
	 */
	private ArrayList<Node> mNodes = new ArrayList<Node>();

	/**
	 * The sorted list of nodes. This is the order in which the animations will
	 * be played. The details about when exactly they will be played depend
	 * on the dependency relationships of the nodes.
	 */
	private ArrayList<Node> mSortedNodes = new ArrayList<Node>();

	/**
	 * Flag indicating whether the nodes should be sorted prior to playing. This
	 * flag allows us to cache the previous sorted nodes so that if the sequence
	 * is replayed with no changes, it does not have to re-sort the nodes again.
	 */
	private boolean mNeedsSort = true;

	private AnimatorSetListener mSetListener = null;

	/**
	 * Flag indicating that the AnimatorSet has been manually
	 * terminated (by calling cancel() or end()).
	 * This flag is used to avoid starting other animations when currently-playing
	 * child animations of this AnimatorSet end. It also determines whether cancel/end
	 * notifications are sent out via the normal AnimatorSetListener mechanism.
	 */
	boolean mTerminated = false;

	/**
	 * Indicates whether an AnimatorSet has been start()'d, whether or
	 * not there is a nonzero startDelay.
	 */
	private boolean mStarted = false;

	// The amount of time in ms to delay starting the animation after start() is called
	private long mStartDelay = 0;

	// Animator used for a nonzero startDelay
	private ValueAnimator mDelayAnim = null;

	// How long the child animations should last in ms. The default value is negative, which
	// simply means that there is no duration set on the AnimatorSet. When a real duration is
	// set, it is passed along to the child animations.
	private long mDuration = -1;


	/**
	 * Returns the current list of child Animator objects controlled by this
	 * AnimatorSet. This is a copy of the internal list; modifications to the returned list
	 * will not affect the AnimatorSet, although changes to the underlying Animator objects
	 * will affect those objects being managed by the AnimatorSet.
	 * 
	 * @return ArrayList<Animator> The list of child animations of this AnimatorSet.
	 */
	public ArrayList<Animator> getChildAnimations() {
		ArrayList<Animator> childList = new ArrayList<Animator>();
		for (Node node : mNodes) {
			childList.add(node.animation);
		}
		return childList;
	}

	/**
	 * Sets the target object for all current {@link #getChildAnimations() child animations} of this AnimatorSet that take targets ({@link ObjectAnimator} and
	 * AnimatorSet).
	 * 
	 * @param target
	 *            The object being animated
	 */
	@Override
	public void setTarget(Object target) {
		for (Node node : mNodes) {
			Animator animation = node.animation;
			if (animation instanceof AnimatorSet) {
				((AnimatorSet) animation).setTarget(target);
			} else if (animation instanceof ObjectAnimator) {
				((ObjectAnimator) animation).setTarget(target);
			}
		}
	}

	/**
	 * Sets the TimeInterpolator for all current {@link #getChildAnimations() child animations} of this AnimatorSet.
	 * 
	 * @param interpolator
	 *            the interpolator to be used by each child animation of this AnimatorSet
	 */
	@Override
	public void setInterpolator(/* Time */Interpolator interpolator) {
		for (Node node : mNodes) {
			node.animation.setInterpolator(interpolator);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Note that canceling a <code>AnimatorSet</code> also cancels all of the animations that it is responsible for.
	 * </p>
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void cancel() {
		mTerminated = true;
		if (isStarted()) {
			ArrayList<AnimatorListener> tmpListeners = null;
			if (mListeners != null) {
				tmpListeners = (ArrayList<AnimatorListener>) mListeners.clone();
				for (AnimatorListener listener : tmpListeners) {
					listener.onAnimationCancel(this);
				}
			}
			if (mDelayAnim != null && mDelayAnim.isRunning()) {
				// If we're currently in the startDelay period, just cancel that animator and
				// send out the end event to all listeners
				mDelayAnim.cancel();
			} else if (mSortedNodes.size() > 0) {
				for (Node node : mSortedNodes) {
					node.animation.cancel();
				}
			}
			if (tmpListeners != null) {
				for (AnimatorListener listener : tmpListeners) {
					listener.onAnimationEnd(this);
				}
			}
			mStarted = false;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Note that ending a <code>AnimatorSet</code> also ends all of the animations that it is responsible for.
	 * </p>
	 */
	@Override
	public void end() {
		mTerminated = true;
		if (isStarted()) {
			if (mSortedNodes.size() != mNodes.size()) {
				// hasn't been started yet - sort the nodes now, then end them
				sortNodes();
				for (Node node : mSortedNodes) {
					if (mSetListener == null)
						mSetListener = new AnimatorSetListener(this);
					node.animation.addListener(mSetListener);
				}
			}
			if (mDelayAnim != null)
				mDelayAnim.cancel();
			if (mSortedNodes.size() > 0)
				for (Node node : mSortedNodes)
					node.animation.end();
			if (mListeners != null) {
				ArrayList<AnimatorListener> tmpListeners = (ArrayList<AnimatorListener>) mListeners.clone();
				for (AnimatorListener listener : tmpListeners)
					listener.onAnimationEnd(this);
			}
			mStarted = false;
		}
	}

	/**
	 * Returns true if any of the child animations of this AnimatorSet have been started and have
	 * not yet ended.
	 * 
	 * @return Whether this AnimatorSet has been started and has not yet ended.
	 */
	@Override
	public boolean isRunning() {
		for (Node node : mNodes)
			if (node.animation.isRunning())
				return true;
		return false;
	}

	@Override
	public boolean isStarted() {
		return mStarted;
	}

	/**
	 * The amount of time, in milliseconds, to delay starting the animation after {@link #start()} is called.
	 * 
	 * @return the number of milliseconds to delay running the animation
	 */
	@Override
	public long getStartDelay() {
		return mStartDelay;
	}

	/**
	 * The amount of time, in milliseconds, to delay starting the animation after {@link #start()} is called.
	 * 
	 * @param startDelay
	 *            The amount of the delay, in milliseconds
	 */
	@Override
	public void setStartDelay(long startDelay) {
		mStartDelay = startDelay;
	}

	/**
	 * Gets the length of each of the child animations of this AnimatorSet. This value may
	 * be less than 0, which indicates that no duration has been set on this AnimatorSet
	 * and each of the child animations will use their own duration.
	 * 
	 * @return The length of the animation, in milliseconds, of each of the child
	 *         animations of this AnimatorSet.
	 */
	@Override
	public long getDuration() {
		return mDuration;
	}

	/**
	 * Sets the length of each of the current child animations of this AnimatorSet. By default,
	 * each child animation will use its own duration. If the duration is set on the AnimatorSet,
	 * then each child animation inherits this duration.
	 * 
	 * @param duration
	 *            The length of the animation, in milliseconds, of each of the child
	 *            animations of this AnimatorSet.
	 */
	@Override
	public AnimatorSet setDuration(long duration) {
		// TODO: don't set the duration of the timing-only nodes created by AnimatorSet to insert "play-after" delays
		if (duration < 0)
			throw new IllegalArgumentException("duration must be a value of zero or greater");
		for (Node node : mNodes)
			node.animation.setDuration(duration);
		mDuration = duration;
		return this;
	}

	@Override
	public void setupStartValues() {
		for (Node node : mNodes)
			node.animation.setupStartValues();
	}

	@Override
	public void setupEndValues() {
		for (Node node : mNodes)
			node.animation.setupEndValues();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * Starting this <code>AnimatorSet</code> will, in turn, start the animations for which it is responsible. The details of when exactly those animations are started depends on the dependency relationships
	 * that have been set up between the animations.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void start() {
		mTerminated = false;
		mStarted = true;

		// First, sort the nodes (if necessary). This will ensure that sortedNodes contains the animation nodes in the correct order.
		sortNodes();

		int numSortedNodes = mSortedNodes.size();
		for (int i = 0; i < numSortedNodes; ++i) {
			Node node = mSortedNodes.get(i);
			// First, clear out the old listeners
			ArrayList<AnimatorListener> oldListeners = node.animation.getListeners();
			if (oldListeners != null && oldListeners.size() > 0) {
				final ArrayList<AnimatorListener> clonedListeners = new ArrayList<AnimatorListener>(oldListeners);
				for (AnimatorListener listener : clonedListeners)
					if (listener instanceof DependencyListener || listener instanceof AnimatorSetListener)
						node.animation.removeListener(listener);
			}
		}

		// nodesToStart holds the list of nodes to be started immediately. We don't want to start the animations in the loop directly because we first need to set up
		// dependencies on all of the nodes. For example, we don't want to start an animation when some other animation also wants to start when the first animation begins.
		final ArrayList<Node> nodesToStart = new ArrayList<Node>();
		for (int i = 0; i < numSortedNodes; ++i) {
			Node node = mSortedNodes.get(i);
			if (mSetListener == null)
				mSetListener = new AnimatorSetListener(this);
			if (node.dependencies == null || node.dependencies.size() == 0)
				nodesToStart.add(node);
			else {
				int numDependencies = node.dependencies.size();
				for (int j = 0; j < numDependencies; ++j) {
					Dependency dependency = node.dependencies.get(j);
					dependency.node.animation.addListener(new DependencyListener(this, node, dependency.rule));
				}
				node.tmpDependencies = (ArrayList<Dependency>) node.dependencies.clone();
			}
			node.animation.addListener(mSetListener);
		}
		// Now that all dependencies are set up, start the animations that should be started.
		if (mStartDelay <= 0) {
			for (Node node : nodesToStart) {
				node.animation.start();
				mPlayingSet.add(node.animation);
			}
		} else {
			mDelayAnim = ValueAnimator.ofFloat(0f, 1f);
			mDelayAnim.setDuration(mStartDelay);
			mDelayAnim.addListener(new AnimatorListenerAdapter() {
				boolean canceled = false;

				public void onAnimationCancel(Animator anim) {
					canceled = true;
				}

				public void onAnimationEnd(Animator anim) {
					if (!canceled) {
						int numNodes = nodesToStart.size();
						for (int i = 0; i < numNodes; ++i) {
							Node node = nodesToStart.get(i);
							node.animation.start();
							mPlayingSet.add(node.animation);
						}
					}
				}
			});
			mDelayAnim.start();
		}
		if (mListeners != null) {
			ArrayList<AnimatorListener> tmpListeners = (ArrayList<AnimatorListener>) mListeners.clone();
			int numListeners = tmpListeners.size();
			for (int i = 0; i < numListeners; ++i)
				tmpListeners.get(i).onAnimationStart(this);
		}
		if (mNodes.size() == 0 && mStartDelay == 0) {
			// Handle unusual case where empty AnimatorSet is started - should send out
			// end event immediately since the event will not be sent out at all otherwise
			mStarted = false;
			if (mListeners != null) {
				ArrayList<AnimatorListener> tmpListeners = (ArrayList<AnimatorListener>) mListeners.clone();
				int numListeners = tmpListeners.size();
				for (int i = 0; i < numListeners; ++i)
					tmpListeners.get(i).onAnimationEnd(this);
			}
		}
	}

	@Override
	public AnimatorSet clone() {
		final AnimatorSet anim = (AnimatorSet) super.clone();
		/*
		 * The basic clone() operation copies all items. This doesn't work very well for
		 * AnimatorSet, because it will copy references that need to be recreated and state
		 * that may not apply. What we need to do now is put the clone in an uninitialized
		 * state, with fresh, empty data structures. Then we will build up the nodes list
		 * manually, as we clone each Node (and its animation). The clone will then be sorted,
		 * and will populate any appropriate lists, when it is started.
		 */
		anim.mNeedsSort = true;
		anim.mTerminated = false;
		anim.mStarted = false;
		anim.mPlayingSet = new ArrayList<Animator>();
		anim.mNodeMap = new HashMap<Animator, Node>();
		anim.mNodes = new ArrayList<Node>();
		anim.mSortedNodes = new ArrayList<Node>();

		// Walk through the old nodes list, cloning each node and adding it to the new nodemap.
		// One problem is that the old node dependencies point to nodes in the old AnimatorSet.
		// We need to track the old/new nodes in order to reconstruct the dependencies in the clone.
		HashMap<Node, Node> nodeCloneMap = new HashMap<Node, Node>(); // <old, new>
		for (Node node : mNodes) {
			Node nodeClone = node.clone();
			nodeCloneMap.put(node, nodeClone);
			anim.mNodes.add(nodeClone);
			anim.mNodeMap.put(nodeClone.animation, nodeClone);
			// Clear out the dependencies in the clone; we'll set these up manually later
			nodeClone.dependencies = null;
			nodeClone.tmpDependencies = null;
			nodeClone.nodeDependents = null;
			nodeClone.nodeDependencies = null;
			// clear out any listeners that were set up by the AnimatorSet; these will
			// be set up when the clone's nodes are sorted
			ArrayList<AnimatorListener> cloneListeners = nodeClone.animation.getListeners();
			if (cloneListeners != null) {
				ArrayList<AnimatorListener> listenersToRemove = null;
				for (AnimatorListener listener : cloneListeners) {
					if (listener instanceof AnimatorSetListener) {
						if (listenersToRemove == null) {
							listenersToRemove = new ArrayList<AnimatorListener>();
						}
						listenersToRemove.add(listener);
					}
				}
				if (listenersToRemove != null) {
					for (AnimatorListener listener : listenersToRemove) {
						cloneListeners.remove(listener);
					}
				}
			}
		}
		// Now that we've cloned all of the nodes, we're ready to walk through their
		// dependencies, mapping the old dependencies to the new nodes
		for (Node node : mNodes) {
			Node nodeClone = nodeCloneMap.get(node);
			if (node.dependencies != null) {
				for (Dependency dependency : node.dependencies) {
					Node clonedDependencyNode = nodeCloneMap.get(dependency.node);
					Dependency cloneDependency = new Dependency(clonedDependencyNode, dependency.rule);
					nodeClone.addDependency(cloneDependency);
				}
			}
		}

		return anim;
	}

	/**
	 * This class is the mechanism by which animations are started based on events in other
	 * animations. If an animation has multiple dependencies on other animations, then
	 * all dependencies must be satisfied before the animation is started.
	 */
	private static class DependencyListener implements AnimatorListener {

		private AnimatorSet mAnimatorSet;

		// The node upon which the dependency is based.
		private Node mNode;

		// The Dependency rule (WITH or AFTER) that the listener should wait for on
		// the node
		private int mRule;

		public DependencyListener(AnimatorSet animatorSet, Node node, int rule) {
			this.mAnimatorSet = animatorSet;
			this.mNode = node;
			this.mRule = rule;
		}

		/**
		 * Ignore cancel events for now. We may want to handle this eventually,
		 * to prevent follow-on animations from running when some dependency
		 * animation is canceled.
		 */
		public void onAnimationCancel(Animator animation) {
		}

		/**
		 * An end event is received - see if this is an event we are listening for
		 */
		public void onAnimationEnd(Animator animation) {
			if (mRule == Dependency.AFTER) {
				startIfReady(animation);
			}
		}

		/**
		 * Ignore repeat events for now
		 */
		public void onAnimationRepeat(Animator animation) {
		}

		/**
		 * A start event is received - see if this is an event we are listening for
		 */
		public void onAnimationStart(Animator animation) {
			if (mRule == Dependency.WITH) {
				startIfReady(animation);
			}
		}

		/**
		 * Check whether the event received is one that the node was waiting for.
		 * If so, mark it as complete and see whether it's time to start
		 * the animation.
		 * 
		 * @param dependencyAnimation
		 *            the animation that sent the event.
		 */
		private void startIfReady(Animator dependencyAnimation) {
			if (mAnimatorSet.mTerminated) {
				// if the parent AnimatorSet was canceled, then don't start any dependent anims
				return;
			}
			Dependency dependencyToRemove = null;
			int numDependencies = mNode.tmpDependencies.size();
			for (int i = 0; i < numDependencies; ++i) {
				Dependency dependency = mNode.tmpDependencies.get(i);
				if (dependency.rule == mRule && dependency.node.animation == dependencyAnimation) {
					// rule fired - remove the dependency and listener and check to
					// see whether it's time to start the animation
					dependencyToRemove = dependency;
					dependencyAnimation.removeListener(this);
					break;
				}
			}
			mNode.tmpDependencies.remove(dependencyToRemove);
			if (mNode.tmpDependencies.size() == 0) {
				// all dependencies satisfied: start the animation
				mNode.animation.start();
				mAnimatorSet.mPlayingSet.add(mNode.animation);
			}
		}

	}

	private class AnimatorSetListener implements AnimatorListener {

		private AnimatorSet mAnimatorSet;

		AnimatorSetListener(AnimatorSet animatorSet) {
			mAnimatorSet = animatorSet;
		}

		public void onAnimationCancel(Animator animation) {
			if (!mTerminated) {
				// Listeners are already notified of the AnimatorSet canceling in cancel().
				// The logic below only kicks in when animations end normally
				if (mPlayingSet.size() == 0) {
					if (mListeners != null) {
						int numListeners = mListeners.size();
						for (int i = 0; i < numListeners; ++i) {
							mListeners.get(i).onAnimationCancel(mAnimatorSet);
						}
					}
				}
			}
		}

		@SuppressWarnings("unchecked")
		public void onAnimationEnd(Animator animation) {
			animation.removeListener(this);
			mPlayingSet.remove(animation);
			Node animNode = mAnimatorSet.mNodeMap.get(animation);
			animNode.done = true;
			if (!mTerminated) {
				// Listeners are already notified of the AnimatorSet ending in cancel() or
				// end(); the logic below only kicks in when animations end normally
				ArrayList<Node> sortedNodes = mAnimatorSet.mSortedNodes;
				boolean allDone = true;
				int numSortedNodes = sortedNodes.size();
				for (int i = 0; i < numSortedNodes; ++i) {
					if (!sortedNodes.get(i).done) {
						allDone = false;
						break;
					}
				}
				if (allDone) {
					// If this was the last child animation to end, then notify listeners that this
					// AnimatorSet has ended
					if (mListeners != null) {
						ArrayList<AnimatorListener> tmpListeners = (ArrayList<AnimatorListener>) mListeners.clone();
						int numListeners = tmpListeners.size();
						for (int i = 0; i < numListeners; ++i) {
							tmpListeners.get(i).onAnimationEnd(mAnimatorSet);
						}
					}
					mAnimatorSet.mStarted = false;
				}
			}
		}

		// Nothing to do
		public void onAnimationRepeat(Animator animation) {
		}

		// Nothing to do
		public void onAnimationStart(Animator animation) {
		}

	}

	/**
	 * This method sorts the current set of nodes, if needed. The sort is a simple
	 * DependencyGraph sort, which goes like this:
	 * - All nodes without dependencies become 'roots'
	 * - while roots list is not null
	 * - for each root r
	 * - add r to sorted list
	 * - remove r as a dependency from any other node
	 * - any nodes with no dependencies are added to the roots list
	 */
	private void sortNodes() {
		if (mNeedsSort) {
			mSortedNodes.clear();
			ArrayList<Node> roots = new ArrayList<Node>();
			int numNodes = mNodes.size();
			for (int i = 0; i < numNodes; ++i) {
				Node node = mNodes.get(i);
				if (node.dependencies == null || node.dependencies.size() == 0) {
					roots.add(node);
				}
			}
			ArrayList<Node> tmpRoots = new ArrayList<Node>();
			while (roots.size() > 0) {
				int numRoots = roots.size();
				for (int i = 0; i < numRoots; ++i) {
					Node root = roots.get(i);
					mSortedNodes.add(root);
					if (root.nodeDependents != null) {
						int numDependents = root.nodeDependents.size();
						for (int j = 0; j < numDependents; ++j) {
							Node node = root.nodeDependents.get(j);
							node.nodeDependencies.remove(root);
							if (node.nodeDependencies.size() == 0) {
								tmpRoots.add(node);
							}
						}
					}
				}
				roots.clear();
				roots.addAll(tmpRoots);
				tmpRoots.clear();
			}
			mNeedsSort = false;
			if (mSortedNodes.size() != mNodes.size()) {
				throw new IllegalStateException("Circular dependencies cannot exist" + " in AnimatorSet");
			}
		} else {
			// Doesn't need sorting, but still need to add in the nodeDependencies list
			// because these get removed as the event listeners fire and the dependencies
			// are satisfied
			int numNodes = mNodes.size();
			for (int i = 0; i < numNodes; ++i) {
				Node node = mNodes.get(i);
				if (node.dependencies != null && node.dependencies.size() > 0) {
					int numDependencies = node.dependencies.size();
					for (int j = 0; j < numDependencies; ++j) {
						Dependency dependency = node.dependencies.get(j);
						if (node.nodeDependencies == null) {
							node.nodeDependencies = new ArrayList<Node>();
						}
						if (!node.nodeDependencies.contains(dependency.node)) {
							node.nodeDependencies.add(dependency.node);
						}
					}
				}
				// nodes are 'done' by default; they become un-done when started, and done
				// again when ended
				node.done = false;
			}
		}
	}

	/**
	 * Dependency holds information about the node that some other node is
	 * dependent upon and the nature of that dependency.
	 * 
	 */
	private static class Dependency {
		static final int WITH = 0; // dependent node must start with this dependency node
		static final int AFTER = 1; // dependent node must start when this dependency node finishes

		// The node that the other node with this Dependency is dependent upon
		public Node node;

		// The nature of the dependency (WITH or AFTER)
		public int rule;

		public Dependency(Node node, int rule) {
			this.node = node;
			this.rule = rule;
		}
	}

	/**
	 * A Node is an embodiment of both the Animator that it wraps as well as
	 * any dependencies that are associated with that Animation. This includes
	 * both dependencies upon other nodes (in the dependencies list) as
	 * well as dependencies of other nodes upon this (in the nodeDependents list).
	 */
	private static class Node implements Cloneable {
		public Animator animation;

		/**
		 * These are the dependencies that this node's animation has on other
		 * nodes. For example, if this node's animation should begin with some
		 * other animation ends, then there will be an item in this node's
		 * dependencies list for that other animation's node.
		 */
		public ArrayList<Dependency> dependencies = null;

		/**
		 * tmpDependencies is a runtime detail. We use the dependencies list for sorting.
		 * But we also use the list to keep track of when multiple dependencies are satisfied,
		 * but removing each dependency as it is satisfied. We do not want to remove
		 * the dependency itself from the list, because we need to retain that information
		 * if the AnimatorSet is launched in the future. So we create a copy of the dependency
		 * list when the AnimatorSet starts and use this tmpDependencies list to track the
		 * list of satisfied dependencies.
		 */
		public ArrayList<Dependency> tmpDependencies = null;

		/**
		 * nodeDependencies is just a list of the nodes that this Node is dependent upon.
		 * This information is used in sortNodes(), to determine when a node is a root.
		 */
		public ArrayList<Node> nodeDependencies = null;

		/**
		 * nodeDepdendents is the list of nodes that have this node as a dependency. This
		 * is a utility field used in sortNodes to facilitate removing this node as a
		 * dependency when it is a root node.
		 */
		public ArrayList<Node> nodeDependents = null;

		/**
		 * Flag indicating whether the animation in this node is finished. This flag
		 * is used by AnimatorSet to check, as each animation ends, whether all child animations
		 * are done and it's time to send out an end event for the entire AnimatorSet.
		 */
		public boolean done = false;

		/**
		 * Constructs the Node with the animation that it encapsulates. A Node has no
		 * dependencies by default; dependencies are added via the addDependency()
		 * method.
		 * 
		 * @param animation
		 *            The animation that the Node encapsulates.
		 */
		public Node(Animator animation) {
			this.animation = animation;
		}

		/**
		 * Add a dependency to this Node. The dependency includes information about the
		 * node that this node is dependency upon and the nature of the dependency.
		 * 
		 * @param dependency
		 */
		public void addDependency(Dependency dependency) {
			if (dependencies == null) {
				dependencies = new ArrayList<Dependency>();
				nodeDependencies = new ArrayList<Node>();
			}
			dependencies.add(dependency);
			if (!nodeDependencies.contains(dependency.node)) {
				nodeDependencies.add(dependency.node);
			}
			Node dependencyNode = dependency.node;
			if (dependencyNode.nodeDependents == null) {
				dependencyNode.nodeDependents = new ArrayList<Node>();
			}
			dependencyNode.nodeDependents.add(this);
		}

		@Override
		public Node clone() {
			try {
				Node node = (Node) super.clone();
				node.animation = (Animator) animation.clone();
				return node;
			} catch (CloneNotSupportedException e) {
				throw new AssertionError();
			}
		}
	}
}