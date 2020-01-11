package ru.napoleonit.summerPlugin

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.*

/**
 * Implements an intention action to replace a ternary statement with if-then-else
 *
 * @author dsl
 */
@NonNls
class StoreByOwnerPropertyIntention : PsiElementBaseIntentionAction(), IntentionAction, PriorityAction {

    private val logger = Logger.getInstance("StoreByOwnerPropertyIntention")

    /**
     * If this action is applicable, returns the text to be shown in the list of
     * intention actions available.
     */
    override fun getText(): String {
        return "storeByOwner"
    }

    /**
     * Returns text for name of this family of intentions. It is used to externalize
     * "auto-show" state of intentions.
     * It is also the directory name for the descriptions.
     *
     * @see com.intellij.codeInsight.intention.IntentionManager.registerIntentionAndMetaData
     * @return  the intention family name.
     */
    override fun getFamilyName(): String {
        return "StoreByOwnerPropertyIntention"
    }

    override fun isAvailable(
        project: Project,
        editor: Editor?,
        element: PsiElement
    ): Boolean {
        val identifier = element as? LeafPsiElement ?: return false
        val property = identifier.parent as? KtProperty ?: return false
        val body = property.parent as? KtClassBody ?: return false
        val interfaze = body.parent as? KtClass ?: return false
        return interfaze.name == "State"
    }

    /**
     * Modifies the Psi to change a ternary expression to an if-then-else statement.
     * If the ternary is part of a declaration, the declaration is separated and
     * moved above the if-then-else statement. Called when user selects this intention action
     * from the available intentions list.
     *
     * @param  project   a reference to the Project object being edited.
     * @param  editor    a reference to the object editing the project source
     * @param  element   a reference to the PSI element currently under the caret
     * @throws IncorrectOperationException Thrown by underlying (Psi model) write action context
     * when manipulation of the psi tree fails.
     * @see StoreByOwnerPropertyIntention.startInWriteAction
     */
    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor, element: PsiElement) {

        val identifier = element as LeafPsiElement
        val propertyName = identifier.text

        val presenterClass = identifier.containingFile.children
            .find {
                (it as? KtClass)?.name?.contains("Presenter") == true
            } as KtClass

        val presenterClassBody = presenterClass.body
        val viewStateProxyProp = presenterClassBody!!.children
            .filterIsInstance<KtProperty>()
            .find { property ->
                property.name == "viewStateProxy"
            } ?: return

        val viewStateProxyObject = viewStateProxyProp.children
            .find {
                (it is KtObjectLiteralExpression)
            } as KtObjectLiteralExpression

        val viewStateProxyObjectBody = viewStateProxyObject.objectDeclaration.children
            .find {
                it is KtClassBody
            } as KtClassBody

        if (viewStateProxyObjectBody.containsPropertyWithName(propertyName)) return

        if (viewStateProxyObjectBody.children.isEmpty()) {
            viewStateProxyObjectBody.clearFromWhitespaces()
        }

        val factory = KtPsiFactory(project)
        val newProperty = factory.createStoreByOwnerProperty(propertyName)
        viewStateProxyObjectBody.addBefore(newProperty, viewStateProxyObjectBody.lastChild)
    }

    /**
     * Indicates this intention action expects the Psi framework to provide the write action
     * context for any changes.
     *
     * @return
     *  *  true if the intention requires a write action context to be provided
     *  *  false if this intention action will start a write action
     *
     */
    override fun startInWriteAction(): Boolean = true

    override fun getPriority(): PriorityAction.Priority = PriorityAction.Priority.TOP
}
